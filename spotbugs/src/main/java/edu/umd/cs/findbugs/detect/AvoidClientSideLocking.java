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
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
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

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.isInsideSynchronizedBlock = false;
        this.currentLockField = null;
        this.methodsUsingLock = new HashSet<>();
        this.methodSynchronizationStatus = new HashMap<>();
        this.inheritedMethods = new HashSet<>();
        this.returnValue = false;
    }

    @Override
    public void visit(JavaClass obj) {
        isInsideSynchronizedBlock = false;
        currentLockField = null;
        super.visit(obj);
    }

    @Override
    public void visitMethod(Method obj) {
        if (!Const.CONSTRUCTOR_NAME.equals(obj.getName())) {
            if (obj.isSynchronized() || methodContainsSynchronizedBlock(getClassName(), obj.getName())) {
                methodsUsingLock.add(getFullyQualifiedMethodName(obj));
                methodSynchronizationStatus.put(getFullyQualifiedMethodName(obj), true);
                System.out.println("Method using locaak: " + getFullyQualifiedMethodName(obj) + " " + methodSynchronizationStatus.get(
                getFullyQualifiedMethodName(obj)) + " " + getClassName() + " " + obj.getName() + " " + methodContainsSynchronizedBlock(
                getClassName(), obj.getName()));
            } 
            System.out.println("Method: " + getFullyQualifiedMethodName(obj) + " isSynchronized: " + obj.isSynchronized() + " Contains synch block "
            + methodContainsSynchronizedBlock(getClassName(), obj.getName()));
            System.out.println("Method using locaakaas: " + getFullyQualifiedMethodName(obj) + " " + methodSynchronizationStatus.get(
                getFullyQualifiedMethodName(obj)) + " " + getClassName() + " " + obj.getName() + " " + methodContainsSynchronizedBlock(
                getClassName(), obj.getName()));
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
                        // System.out.println("Opcode: " + insn.getOpcode());
                    }
                }
                // System.out.println("Instuctions: " + mn.instructions.toString());
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
            // System.out.println(getFullyQualifiedMethodName(getMethod()) + " METHOD I AM IN " + methodSynchronizationStatus.get(getFullyQualifiedMethodName(getMethod())));
            if (stack.getStackDepth() > 0) {
                OpcodeStack.Item top = stack.getStackItem(0);
                XField field = top.getXField();
                XMethod methodC = top.getReturnValueOf();
                if (isMethodInherited(methodC)) {
                    inheritedMethods.add(getFullyQualifiedMethodName(methodC));
                }
                // System.out.println(methodC + " gggg");
                if (field != null) {
                    currentLockField = field;
                    System.out.println("Fielda: "+ field.getClassName());
                    if (isLockObject(top)) {
                        methodsUsingLock.add(getFullyQualifiedMethodName(getMethod()));
                        System.out.println("ewewewew " + getFullyQualifiedMethodName(getMethod()) + " " + methodSynchronizationStatus.get(
                            getFullyQualifiedMethodName(getMethod())));
                    }
                    System.out.println("Method usings: " + getFullyQualifiedMethodName(getMethod()) + " " + methodSynchronizationStatus.get(
                    getFullyQualifiedMethodName(getMethod())));
                    System.out.println("Class MNName: " + getClassNameFromFieldDescriptor(field));
                    if (isInsideSynchronizedBlock && currentLockField == null) {
                        reportViolation(methodsUsingLock);
                    }

                }
                if (methodC != null && !Const.CONSTRUCTOR_NAME.equals(methodC.getName()) && !isMethodInherited(getMethod()) && isMethodInherited(
                        methodC)) {
                    System.out.println("Method using return value:" + getFullyQualifiedMethodName(getMethod()) + " ss " + getFullyQualifiedMethodName(
                            methodC) + " ss " + getMethod());
                    if (isInsideSynchronizedBlock && currentLockField == null) {
                        System.out.println(isMethodInherited(methodC) + " inherited " + methodC);
                        if (isMethodInherited(methodC)) {
                            inheritedMethods.add(getFullyQualifiedMethodName(methodC));
                        }
                        System.out.println(isMethodInherited(getMethod()) + " inheriteds " + getMethod());
                        // methodsUsingLock.add(getFullyQualifiedMethodName(getMethod()));
                        methodSynchronizationStatus.put(getFullyQualifiedMethodName(getMethod()), false);
                        // reportViolation(methodsUsingLock);
                        Boolean isSynchronized = methodSynchronizationStatus.get(getFullyQualifiedMethodName(getMethod()));
                        if (isSynchronized == null || !isSynchronized.booleanValue()) {
                            bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                                    .addClassAndMethod(this)
                                    .addString("Not all methods using the lock are synchronized"));
                            System.out.println("inside m " + this + " " + methodSynchronizationStatus.get(getFullyQualifiedMethodName(getMethod())));
                            returnValue = true;
                        }
                    }
                }
            }
            // print methodUsingLock
            // for (String method : methodsUsingLock) {
            //     System.out.println("Method using lockaaa: " + methodSynchronizationStatus.get(method) + " " + method);
            // }
            for (String method : inheritedMethods) {
                System.out.println("Method using locka inher: " + methodSynchronizationStatus.get(method) + " " + method);
            }
        } else if (seen == Const.MONITOREXIT) {
            isInsideSynchronizedBlock = false;
        } else if (seen == Const.PUTFIELD || seen == Const.GETFIELD) {
            System.out.println("Curr lock f: " + currentLockField + " " + getFullyQualifiedMethodName(getMethod()) );
                if (!Const.CONSTRUCTOR_NAME.equals(getMethod().getName()) && !methodsUsingLock.contains(getFullyQualifiedMethodName(getMethod())) && !returnValue) {
                    methodsUsingLock.add(getFullyQualifiedMethodName(getMethod()));
                    System.out.println("Method using locks: " + getFullyQualifiedMethodName(getMethod()));
                    reportViolation(methodsUsingLock);
                }
    }
}

    private static boolean isInterestingField(ClassMember classMember) {
        if (classMember == null) {
            return false;
        }
        Set<String> interestingCollectionMethodNames = new HashSet<>(Arrays.asList(
                "synchronizedCollection", "synchronizedSet", "synchronizedSortedSet",
                "synchronizedNavigableSet", "synchronizedList", "synchronizedMap",
                "synchronizedSortedMap", "synchronizedNavigableMap"));
        return ("java.util.Collections".equals(classMember.getClassName()) && interestingCollectionMethodNames.contains(classMember.getName()))
                || (classMember.getClassName().startsWith("java.util.concurrent.atomic") && classMember.getSignature().endsWith(")V"));
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
                // bugReporter.reportMissingClass(e);
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
                // bugReporter.reportMissingClass(e);
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
            // System.out.println("Potential lock: " + getClassNameFromFieldDescriptor(potentialLock) + " "
            // + potentialLock.equals(currentLockField) + " " + currentLockField); // + " " +
            // currentJavaClass.getClassName()
            // + " equals: " +
            // potentialLock.equals(currentLockField));
            return potentialLock.equals(currentLockField);
        }
        return false;
    }

    private String getClassNameFromFieldDescriptor(XField field) {
        String fieldDescriptor = field.getFieldDescriptor().toString();
        int lastIndex = fieldDescriptor.lastIndexOf('.');
        if (lastIndex != -1) {
            String className = fieldDescriptor.substring(lastIndex + 1);
            System.out.println("Class name: " + className);
            return className;
        }
        return null;
    }

    public void reportViolation(HashSet<String> methodsUsingLock) {
        for (String method : methodsUsingLock) {
            System.out.println("Method using lockas: " + methodSynchronizationStatus.get(method) + " " + method);
        }
        System.out.println("+++++++++++++++++");
        for (String methodSignature : methodsUsingLock) {
            System.out.println("Method using lock: " + methodSignature + " " + methodSynchronizationStatus.get(methodSignature));
            Boolean isSynchronized = methodSynchronizationStatus.get(methodSignature);
            System.out.println("issynchrr " + isSynchronized + " " + methodSignature);
            if (isSynchronized == null || !isSynchronized.booleanValue()) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                        .addClass(this).addMethod(this.getXMethod())
                        .addString("Not all methods using the lock are synchronized"));
                System.out.println("asdasd " + this.getXMethod() + " " + methodSynchronizationStatus.get(methodSignature));
            }

        }
        for (String method : methodsUsingLock) {
            // System.out.println("Method using locke: " + methodSynchronizationStatus.get(method) + " " + method);
        }
    }

    public void reportViolationInherited(HashSet<String> methodsUsingLock) {
        for (String methodSignature : methodsUsingLock) {
            // System.out.println("Method using locksa: " + methodSignature);
            boolean isSynchronized = methodSynchronizationStatus.get(methodSignature);
            if (isSynchronized) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                        .addClassAndMethod(this)
                        .addString("Do not use synchronization in inherited methods"));
                // System.out.println("treeee " + this + " " + methodSynchronizationStatus.get(methodSignature));
            }

        }
        for (String method : methodsUsingLock) {
            // System.out.println("Method using locker: " + methodSynchronizationStatus.get(method) + " " + method);
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
