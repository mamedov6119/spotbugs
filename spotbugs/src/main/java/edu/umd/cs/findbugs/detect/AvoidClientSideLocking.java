package edu.umd.cs.findbugs.detect;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
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

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.isInsideSynchronizedBlock = false;
        this.currentLockField = null;
        this.methodsUsingLock = new HashSet<>();
        this.methodSynchronizationStatus = new HashMap<>();
        this.inheritedMethods = new HashSet<>();
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
                System.out.println("Method using locaak: " + getFullyQualifiedMethodName(obj) + " " + methodSynchronizationStatus.get(getFullyQualifiedMethodName(obj)) + " " + getClassName() + " " + obj.getName() + " " + methodContainsSynchronizedBlock(getClassName(), obj.getName()));
            }
            System.out.println("Method: " + getFullyQualifiedMethodName(obj) + " isSynchronized: " + obj.isSynchronized() + " Contains synch block " + methodContainsSynchronizedBlock(getClassName(), obj.getName()) + " is using synch col: ");
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
                    if (insn.getOpcode() == Opcodes.MONITORENTER) {
                        return true;
                    }
                    System.out.println("Opcode: "+insn.getOpcode());
                }
            }
                System.out.println("Instuctions: "+mn.instructions.toString());
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
                System.out.println(methodC + " gggg");
                if (field != null) {
                    currentLockField = field;
                    if (isLockObject(top)) {
                        methodsUsingLock.add(getFullyQualifiedMethodName(getMethod()));
                    }
                    System.out.println("Method using: " + getFullyQualifiedMethodName(getMethod()) + " " + methodSynchronizationStatus.get(getFullyQualifiedMethodName(getMethod())));
                    if (isInsideSynchronizedBlock && currentLockField == null) {
                        reportViolation(methodsUsingLock);
                    }
                }
                if (methodC != null && !Const.CONSTRUCTOR_NAME.equals(methodC.getName())) {
                    System.out.println("Method using return value:" + getFullyQualifiedMethodName(getMethod()) + " ss " + getFullyQualifiedXMethodName(methodC) + " ss " + getMethod());
                    if (isInsideSynchronizedBlock && currentLockField == null && isXMethodInherited(methodC)) {
                        System.out.println(isXMethodInherited(methodC) + " inherited " + methodC);
                        System.out.println(isMethodInherited(getMethod()) + " inherited " + getMethod());
                        methodsUsingLock.add(getFullyQualifiedXMethodName(methodC));
                        reportViolation(methodsUsingLock);
                    }
                }
            }
            // print methodUsingLock
            for(String method : methodsUsingLock) {
                System.out.println("Method using locka: " + methodSynchronizationStatus.get(method) + " " + method);
            }
            for(String method : inheritedMethods) {
                System.out.println("Method using locka inher: " + methodSynchronizationStatus.get(method) + " " + method);
            }
        } else if (seen == Const.MONITOREXIT) {
            currentLockField = null;
        } else if (seen == Const.PUTFIELD || seen == Const.GETFIELD) {
            if (!Const.CONSTRUCTOR_NAME.equals(getMethod().getName()) && !methodsUsingLock.contains(getFullyQualifiedMethodName(getMethod()))) {
                methodsUsingLock.add(getFullyQualifiedMethodName(getMethod()));
                System.out.println("Method using locks: " + getFullyQualifiedMethodName(getMethod()));
                
                reportViolation(methodsUsingLock);
            }
        } 
    }

    private boolean isXMethodInherited(XMethod method) {
        try {
            String className = method.getClassName();
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
                    if (method.getName().equals(superClassMethod.getName()) &&
                        method.getSignature().equals(superClassMethod.getSignature())) {
                        return true;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }
    
        return false;
    }



    private boolean isMethodInherited(Method method) {
        try {
            String className = method.getName();
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
                    if (method.getName().equals(superClassMethod.getName()) &&
                        method.getSignature().equals(superClassMethod.getSignature())) {
                        return true;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }
    
        return false;
    }

    private boolean isLockObject(Item item) {
        if (!item.getXField().isReferenceType()) {
            return false;
        }
        XField potentialLock = item.getXField();
        if (potentialLock != null) {
            System.out.println("Potential lock: " + getClassNameFromFieldDescriptor(potentialLock) + " "
                    + potentialLock.equals(currentLockField) + " " + currentLockField); // + " " +
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
            // System.out.println("Class name: " + className);
            return className;
        }
        return null;
    }

    public void reportViolation(HashSet<String> methodsUsingLock) {
        for (String methodSignature : methodsUsingLock) {
            System.out.println("Method using lock: " + methodSignature);
            Boolean isSynchronized = methodSynchronizationStatus.get(methodSignature);
            if (isSynchronized == null || !isSynchronized.booleanValue()) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", HIGH_PRIORITY)
                        .addClassAndMethod(this)
                        .addString("Not all methods using the lock are synchronized"));
                System.out.println("asdasd "+this + " " + methodSynchronizationStatus.get(methodSignature));
            }

        }
        for(String method : methodsUsingLock) {
            System.out.println("Method using locke: " + methodSynchronizationStatus.get(method) + " " + method);
        }
    }

    private String getFullyQualifiedMethodName(Method method) {
        return getClassName() + "." + method.getName() + method.getSignature();
    }
    private String getFullyQualifiedXMethodName(XMethod method) {
        return getClassName() + "." + method.getName() + method.getSignature();
    }

}
