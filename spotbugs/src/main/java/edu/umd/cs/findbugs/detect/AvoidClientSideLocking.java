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
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.LVTHelper;


public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private boolean isInsideSynchronizedBlock;
    private XField currentLockField;
    private Map<String, Boolean> synchronizedMethodsMap;
    private JavaClass currentJavaClass;
    private Set<XField> reportedViolations = new HashSet<>();
    private LocalVariable lv;
    private LocalVariableTable lvt;

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.isInsideSynchronizedBlock = false;
        this.currentLockField = null;
        this.synchronizedMethodsMap = new HashMap<>();
        this.currentJavaClass = null;
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
        if (obj.isSynchronized()) {
            String methodSignature = currentJavaClass.getClassName() + "." + obj.getSignature();
            System.out.println("Synchronized method: " + methodSignature + " in class " + currentJavaClass.getClassName());
            synchronizedMethodsMap.put(methodSignature, true);
        }
        lvt = getMethod().getLocalVariableTable();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            isInsideSynchronizedBlock = true;
            if (stack.getStackDepth() > 0) {
                Item top = stack.getStackItem(0);
                XField field = top.getXField();
                XMethod methodC = top.getReturnValueOf();

                if (field != null) {
                    currentLockField = field;
                    String className = field.getClassName();
                    if (!classHasSynchronizedMethods(className)) {
                        checkAndReportViolation(field);
                    }
                } else if (methodC != null && methodC.getName().equals("<init>")) {
                    String className = methodC.getClassName();
                    if (!classHasSynchronizedMethods(className)) {
                        checkAndReportViolationFromConstructor(methodC);
                    }
                } else if (lvt != null) {
                    String className = "";
                    for (LocalVariable localVariable : lvt) {
                        if (localVariable.getStartPC() <= getPC() && localVariable.getStartPC() + localVariable.getLength() > getPC()) {
                            className = localVariable.getClass().getName();
                            // lv = localVariable;
                            System.out.println("Local variable: " + localVariable.getName() + " " + className);
                        } // HOW TO GET THE SPECIFICLY FROM SYNCHRONIZED(______) VARIABLE????
                    }
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

    private boolean classHasSynchronizedMethods(String className) {
        //synch block as well
        System.out.println("Checking if class " + className + " has synchronized methods. " + synchronizedMethodsMap.keySet().stream()
                .anyMatch(signature -> signature.startsWith(className + ".")));

        return synchronizedMethodsMap.keySet().stream()
                .anyMatch(signature -> signature.startsWith(className + "."));
    }

    private void checkAndReportViolation(XField lockField) {
        // implement logic to check for violations and then report bugs
        // compare lockField with the class's declared locking strategy
        // report a bug if the client-side locking is detected
        if (isInsideSynchronizedBlock && currentLockField != null) {
            String lockFieldClassName = lockField.getClassName();

            boolean hasSynchronnizedMethods = classHasSynchronizedMethods(lockFieldClassName);

            if (!hasSynchronnizedMethods) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY).addClassAndMethod(this)
                        .addField(lockField)
                        .addString("Avoid client-side locking when using classes that do not commit to their locking strategy"));
            }
        }
    }

    private void checkAndReportViolationFromConstructor(XMethod constructorMethod) {
        // check if the class containing the constructor has synchronized methods
        String className = constructorMethod.getClassName();
        if (!classHasSynchronizedMethods(className)) {
            bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addMethod(constructorMethod)
                    .addString("Avoid client-side locking when using classes that do not commit to their locking strategy"));
        }
    }

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
