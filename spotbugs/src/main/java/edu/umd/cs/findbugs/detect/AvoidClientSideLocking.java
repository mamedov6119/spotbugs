package edu.umd.cs.findbugs.detect;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import org.apache.bcel.classfile.LocalVariable;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private boolean isInsideSynchronizedBlock;
    private HashSet<String> methodsUsingLock;
    private HashSet<String> inheritedMethods;
    private Map<String, Boolean> methodSynchronizationStatus;
    private XField currentLockField;
    private boolean returnValue;
    private LocalVariable currentLocalVariable;
    private LocalVariableTable localVariableTable;

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.isInsideSynchronizedBlock = false;
        this.currentLockField = null;
        this.methodsUsingLock = new HashSet<>();
        this.methodSynchronizationStatus = new HashMap<>();
        this.inheritedMethods = new HashSet<>();
        this.returnValue = false;
        this.currentLocalVariable = null;
        this.localVariableTable = null;
    }

    @Override
    public void visitMethod(Method obj) {
        if (!Const.CONSTRUCTOR_NAME.equals(obj.getName())) {
            if (obj.isSynchronized() || methodContainsSynchronizedBlock(getClassName(), obj.getName())) {
                methodsUsingLock.add(getFullyQualifiedMethodName(obj));
                methodSynchronizationStatus.put(getFullyQualifiedMethodName(obj), true);
                localVariableTable = obj.getLocalVariableTable();
            }
        }
        super.visitMethod(obj);
    }

    public boolean methodContainsSynchronizedBlock(String className, String methodName) {
        try {
            ClassReader cr = new ClassReader(className);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            for (MethodNode mn : cn.methods) {
                if (mn.name.equals(methodName)) {
                    for (AbstractInsnNode insn : mn.instructions.toArray()) {
                        if ((insn.getOpcode() == Opcodes.MONITORENTER)) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            isInsideSynchronizedBlock = true;
            if (stack.getStackDepth() > 0) {
                OpcodeStack.Item top = stack.getStackItem(0);
                XField field = top.getXField();
                XMethod methodC = top.getReturnValueOf();
                if (field != null) {
                    currentLockField = field;
                    if (isLockObject(top)) {
                        methodsUsingLock.add(getFullyQualifiedMethodName(getMethod()));
                    }
                    if (isInsideSynchronizedBlock && currentLockField == null) {
                        reportViolation(methodsUsingLock);
                    }

                }
                if (methodC != null && !Const.CONSTRUCTOR_NAME.equals(methodC.getName()) && !isMethodInherited(getMethod()) && isMethodInherited(
                        methodC)) {
                    if (isInsideSynchronizedBlock && currentLockField == null) {
                        if (isMethodInherited(methodC)) {
                            inheritedMethods.add(getFullyQualifiedMethodName(methodC));
                        }
                        methodSynchronizationStatus.put(getFullyQualifiedMethodName(getMethod()), false);
                        Boolean isSynchronized = methodSynchronizationStatus.get(getFullyQualifiedMethodName(getMethod()));
                        if (isSynchronized == null || !isSynchronized.booleanValue()) {
                            bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                                    .addClassAndMethod(this)
                                    .addString("Not all methods using the lock are synchronized"));
                            returnValue = true;
                        }
                    }
                }
                if (localVariableTable != null) {
                    currentLocalVariable = getLockVariableFromStack(stack);
                }

            }
        } else if (seen == Const.MONITOREXIT) {
            isInsideSynchronizedBlock = false;
        } else if (seen == Const.PUTFIELD || seen == Const.GETFIELD) {
            if (!Const.CONSTRUCTOR_NAME.equals(getMethod().getName()) && !methodsUsingLock.contains(getFullyQualifiedMethodName(getMethod()))
                    && !returnValue) {
                methodsUsingLock.add(getFullyQualifiedMethodName(getMethod()));
                reportViolation(methodsUsingLock);
            }
        } else if (seen == Const.ALOAD) {
            bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                    .addClassAndMethod(this)
                    .addString("Local variable used as lock"));
        }
    }

    private LocalVariable getLockVariableFromStack(OpcodeStack stack) {
        if (stack.getStackDepth() > 0) {
            Item topItem = stack.getStackItem(0);
            if (topItem != null && topItem.isInitialParameter()) {
                return null; // This means the object is a method parameter, not a local variable.
            }

            int localVarIndex = topItem.getRegisterNumber(); // Get the local variable index.

            if (localVariableTable != null && localVarIndex >= 0) {
                // Iterate over the local variable table to find the matching variable.
                for (LocalVariable lv : localVariableTable.getLocalVariableTable()) {
                    if (lv.getIndex() == localVarIndex) {
                        return lv; // This is the local variable used as a lock.
                    }
                }
            }
        }
        return null;
    }


    private static boolean isInterestingField(ClassMember classMember) {
        if (classMember == null) {
            return false;
        }
        Set<String> interestingCollectionMethodNames = new HashSet<>(Arrays.asList(
                "synchronizedCollection", "synchronizedSet", "synchronizedSortedSet",
                "synchronizedNavigableSet", "synchronizedList", "synchronizedMap",
                "synchronizedSortedMap", "synchronizedNavigableMap"));
        return ("java.util.Collections".equals(classMember.getClassName())
                && interestingCollectionMethodNames.contains(classMember.getName()))
                || (classMember.getClassName().startsWith("java.util.concurrent.atomic")
                        && classMember.getSignature().endsWith(")V"));
    }

    private boolean isMethodInherited(Object method) {
        if (method instanceof Method) {
            try {
                String className = ((Method) method).getName();
                JavaClass declaringClass = Repository.lookupClass(className);
                JavaClass currentClass = getClassContext().getJavaClass();

                if (declaringClass.equals(currentClass)) {
                    return false;
                }

                while (currentClass != null && !currentClass.equals(declaringClass)) {
                    currentClass = currentClass.getSuperClass();

                    if (currentClass == null) {
                        break;
                    }

                    for (Method superClassMethod : currentClass.getMethods()) {
                        if (((Method) method).getName().equals(superClassMethod.getName()) &&
                                ((Method) method).getSignature().equals(superClassMethod.getSignature())) {
                            return true;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                bugReporter.reportMissingClass(e);
            }
        } else if (method instanceof XMethod) {
            try {
                String className = ((XMethod) method).getClassName();
                JavaClass declaringClass = Repository.lookupClass(className);
                JavaClass currentClass = getClassContext().getJavaClass();

                if (declaringClass.equals(currentClass)) {
                    return false;
                }

                while (currentClass != null && !currentClass.equals(declaringClass)) {
                    currentClass = currentClass.getSuperClass();

                    if (currentClass == null) {
                        break;
                    }

                    for (Method superClassMethod : currentClass.getMethods()) {
                        if (((XMethod) method).getName().equals(superClassMethod.getName()) &&
                                ((XMethod) method).getSignature().equals(superClassMethod.getSignature())) {
                            return true;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                bugReporter.reportMissingClass(e);
            }
        }
        return false;
    }

    private boolean isLockObject(Item item) {
        if (!item.getXField().isReferenceType()) {
            return false;
        }
        XField potentialLock = item.getXField();
        if (potentialLock != null) {
            return potentialLock.equals(currentLockField);
        }
        return false;
    }

    public void reportViolation(HashSet<String> methodsUsingLock) {
        for (String methodSignature : methodsUsingLock) {
            Boolean isSynchronized = methodSynchronizationStatus.get(methodSignature);
            if (isSynchronized == null || !isSynchronized.booleanValue()) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                        .addClass(this).addMethod(this.getXMethod())
                        .addString("Not all methods using the lock are synchronized"));
            }

        }
    }

    public void reportViolationInherited(HashSet<String> methodsUsingLock) {
        for (String methodSignature : methodsUsingLock) {
            boolean isSynchronized = methodSynchronizationStatus.get(methodSignature);
            if (isSynchronized) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                        .addClassAndMethod(this)
                        .addString("Do not use synchronization in inherited methods"));
            }

        }
    }

    private String getFullyQualifiedMethodName(Object method) {
        if (method instanceof Method) {
            return getClassName() + "." + ((Method) method).getName() + ((Method) method).getSignature();
        } else if (method instanceof XMethod) {
            return getClassName() + "." + ((XMethod) method).getName() + ((XMethod) method).getSignature();
        }
        return null;
    }
}
