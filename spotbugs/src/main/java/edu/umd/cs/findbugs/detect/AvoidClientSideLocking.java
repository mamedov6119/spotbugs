package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Const;
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


public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private boolean isInsideSynchronizedBlock;
    private XField currentLockField;
    private Map<String, Boolean> synchronizedMethodsMap;
    private JavaClass currentJavaClass;
    private String currentMethodSignature;
    private Set<XField> reportedViolations = new HashSet<>();
    private LocalVariable lv;
    private LocalVariableTable lvt;

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.isInsideSynchronizedBlock = false;
        this.currentLockField = null;
        this.synchronizedMethodsMap = new HashMap<>();
        this.currentJavaClass = null;
        this.currentMethodSignature = null;
    }

    @Override
    public void visit(JavaClass obj) {
        isInsideSynchronizedBlock = false;
        currentLockField = null;
        currentJavaClass = obj;
        super.visit(obj);
    }

    @Override
    public void visitMethod(Method obj) {
        String currentMethodSignature = currentJavaClass.getClassName() + "." + obj.getSignature();
        if (obj.isSynchronized()) {
            System.out.println("Synchronized method: " + currentMethodSignature + " in class " + currentJavaClass.getClassName());
            synchronizedMethodsMap.put(currentMethodSignature, true);
        }
        // synchronizedMethodsMap.put(currentMethodSignature, false);
        lvt = getMethod().getLocalVariableTable();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            isInsideSynchronizedBlock = true;
            if (stack.getStackDepth() > 0) {
                Item top = stack.getStackItem(0);
                // System.out.println("Top: " + top.getReturnValueOf());
                XField field = top.getXField();
                XMethod methodC = top.getReturnValueOf();
                if (field != null) {
                    currentLockField = field;
                    System.out.println("Field: " + getClassNameFromFieldDescriptor(field) + " " + currentJavaClass.getClassName());
                    checkAndReportViolation(field); // report the non synchronied methods with usage of the lock objct
                } else if (methodC != null && Const.CONSTRUCTOR_NAME.equals(methodC.getName())) {
                    String className = methodC.getClassName();
                    if (!classHasSynchronizedMethod(className)) {
                        // checkAndReportViolationFromConstructor(methodC);
                    }
                } else if (lvt != null) {
                    String className = "";
                    for (LocalVariable localVariable : lvt) {
                        if (localVariable.getStartPC() <= getPC() && localVariable.getStartPC() + localVariable.getLength() > getPC()) {
                            className = localVariable.getClass().getName();
                            // lv = localVariable;
                            System.out.println("Local variable: " + localVariable.getName() + " " + className);
                        } // HOW TO GET THE SPECIFICLY FROM SYNCHRONIZED(______) VARIABLE????
                    } // Assume its a field and check other test cases.
                }

            } else if (seen == Const.MONITOREXIT) {
                isInsideSynchronizedBlock = false;
            } else if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
                if (isInsideSynchronizedBlock && currentLockField != null && !reportedViolations.contains(currentLockField)) {
                    checkAndReportViolation(currentLockField);
                    reportedViolations.add(currentLockField);
                }
            }
        }
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

    private boolean classHasSynchronizedMethod(String className) {
        //synch block as well
        // System.out.println("Checking if class " + className + " has synchronized methods. " + synchronizedMethodsMap.keySet().stream()
        //         .anyMatch(signature -> signature.startsWith(className + ".")));
        if (synchronizedMethodsMap.values().contains(false)) {
            return true;
        }
        if (className.contains("/")) {
            return synchronizedMethodsMap.keySet().stream()
                .anyMatch(signature -> signature.startsWith(className + "/"));
        } else {
            return synchronizedMethodsMap.keySet().stream()
                .anyMatch(signature -> signature.startsWith(className + "."));
        }
    }

    private boolean classHasUnsynchronizedMethods(String className) {
        for (Map.Entry<String, Boolean> entry : synchronizedMethodsMap.entrySet()) {
            if ((entry.getKey().startsWith(className + "/") || entry.getKey().startsWith(className + ".")) && !entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    private void checkAndReportViolation(XField lockField) {
        // implement logic to check for violations and then report bugs
        // compare lockField with the class's declared locking strategy
        // report a bug if the client-side locking is detected
        // if (isInsideSynchronizedBlock && currentLockField != null) {
            String lockFieldClassName = lockField.getClassName();

            boolean hasSynchronnizedMethods = classHasSynchronizedMethod(lockFieldClassName) && !classHasUnsynchronizedMethods(currentJavaClass.getClassName());

            if (!hasSynchronnizedMethods) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY).addClassAndMethod(this)
                        .addField(lockField)
                        .addString("Avoid client-side locking when using classes that do not commit to their locking strategy"));
            }
        // }
    }

    // private void checkAndReportViolationFromConstructor(XMethod constructorMethod) {
    //     // check if the class containing the constructor has synchronized methods
    //     String className = constructorMethod.getClassName();
    //     if (!classHasSynchronizedMethods(className)) {
    //         bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY)
    //                 .addClassAndMethod(this)
    //                 .addMethod(constructorMethod)
    //                 .addString("Avoid client-side locking when using classes that do not commit to their locking strategy"));
    //     }
    // }

    private void reportLocalVariableViolation(LocalVariable localVariable) {
        if (isInsideSynchronizedBlock && currentLockField != null) {
            // Extract necessary information from the local variable
            String variableName = localVariable.getName();
            // String variableType = localVariable.getSignature(); // variable type
            int lineNumber = localVariable.getStartPC(); // get the start PC or line number of the variable

            // Construct a BugInstance to report the violation
            bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY)
                    .addClass(currentJavaClass).addField(currentLockField)
                    .addString("Avoid using local variables as locks, found: " + variableName)
                    .addSourceLine(this, lineNumber));
        }
    }

}
