package edu.umd.cs.findbugs.detect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.util.SyntheticRepository;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;

public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private boolean isFirstVisit = false;
    private String currentPackageName = null;
    private XField currentLockField = null;
    private XMethod returnMethodUsedAsLock = null;
    private final Set<Method> unsynchronizedMethods = new HashSet<>();
    private final Map<Method, LocalVariableAnnotation> localVariableAnnotationsMap = new HashMap<>();
    private final Set<XField> threadSafeFields = new HashSet<>();

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(JavaClass jc) {
        currentPackageName = jc.getPackageName();
        isFirstVisit = true;
        Method[] methods = jc.getMethods();
        for (Method obj : methods) {
            if (!Const.CONSTRUCTOR_NAME.equals(obj.getName()) && !Const.STATIC_INITIALIZER_NAME.equals(obj.getName())) {
                if (obj.isSynchronized()) {
                    getLocalVariableTableOfMethod();
                } else {
                    unsynchronizedMethods.add(obj);
                    doVisitMethod(obj);
                }
            } else {
                doVisitMethod(obj);
            }
        }

        isFirstVisit = false;
        super.visit(jc);
    }

    @Override
    public void sawOpcode(int seen) {
        if (isFirstVisit) {
            if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
                if (Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                    XField xfield = getXFieldOperand();
                    if (xfield != null) {
                        Item value = stack.getStackItem(0);
                        if (value.getReturnValueOf() != null) {
                        }
                        if (value != null && (value.getSignature() != null && isThreadSafeField(value.getSignature()) || value
                                .getReturnValueOf() != null && isThreadSafeField(value.getReturnValueOf().getName()))) {
                            threadSafeFields.add(xfield);
                        }
                    }
                }
            }

            if (seen == Const.MONITORENTER) {
                if (stack.getStackDepth() > 0) {
                    Item topItem = stack.getStackItem(0);
                    if (topItem.getXField() != null) {
                        XField xfield = topItem.getXField();
                        if (xfield.getClassName().equals(getThisClass().getClassName())
                                && !threadSafeFields.contains(xfield)) {
                            currentLockField = xfield;
                        }
                    }
                    if (unsynchronizedMethods.contains(getMethod())) {
                        unsynchronizedMethods.remove(getMethod());
                    }
                    LocalVariableAnnotation localVariableAnnotation = LocalVariableAnnotation
                            .getLocalVariableAnnotation(getMethod(), stack.getStackItem(0), getPC());
                    if (localVariableAnnotation != null) {
                        localVariableAnnotationsMap.put(getMethod(), localVariableAnnotation);
                    }
                }
            }
        } else {
            detectLockingProblems(seen);
        }
    }

    private boolean isThreadSafeField(String signature) {
        Set<String> interestingCollectionMethodNames = new HashSet<>(Arrays.asList(
                "synchronizedCollection", "synchronizedSet", "synchronizedSortedSet",
                "synchronizedNavigableSet", "synchronizedList", "synchronizedMap",
                "synchronizedSortedMap", "synchronizedNavigableMap"));

        return signature.contains("Ljava/util/concurrent/") || interestingCollectionMethodNames.contains(signature);

    }

    private LocalVariableTable getLocalVariableTableOfMethod() {
        if (stack != null && stack.getStackDepth() > 0) {
            return getMethod().getLocalVariableTable();
        }
        return null;
    }

    private void detectLockingProblems(int seen) {
        if (seen == Const.PUTFIELD || seen == Const.GETFIELD || seen == Const.GETSTATIC || seen == Const.PUTSTATIC) {
            if (!Const.CONSTRUCTOR_NAME.equals(getMethodName())
                    && !Const.STATIC_INITIALIZER_NAME.equals(getMethodName())) {
                if (getXFieldOperand() != null && currentLockField != null) {
                    XField xfield = getXFieldOperand();
                    if (((xfield.equals(currentLockField) && unsynchronizedMethods.contains(getMethod()))
                            || (xfield.isStatic() && currentPackageName
                                    .equals(xfield.getPackageName()) && unsynchronizedMethods.contains(getMethod())))
                            && !threadSafeFields.contains(xfield)) {
                        reportClassFieldBug(getThisClass(), getMethod(),
                                SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                                        getPC()));
                    }

                }
            }
        }

        if (seen == Const.MONITORENTER) {
            if (getLocalVariableTableOfMethod() != null) {
                org.apache.bcel.util.Repository repository = SyntheticRepository.getInstance();
                LocalVariable localVariable = getLockVariableFromStack(stack);
                if (localVariable != null) {
                    JavaClass localVarClass = null;
                    try {
                        localVarClass = repository
                                .loadClass(ClassName.fromFieldSignatureToDottedClassName(localVariable.getSignature()));
                        if (localVarClass != null && localVarClass.getPackageName().equals(currentPackageName)) {
                            reportLocalVarBug(getThisClass(), getMethod(),
                                    SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                                            getPC()));
                        }
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                    }
                }
            }

            if (stack.getStackDepth() > 0) {
                XMethod methodC = stack.getStackItem(0).getReturnValueOf();
                org.apache.bcel.util.Repository repository = SyntheticRepository.getInstance();
                JavaClass methodClass = null;
                if (methodC != null && !Const.CONSTRUCTOR_NAME.equals(methodC.getName())
                        && !Const.STATIC_INITIALIZER_NAME.equals(methodC.getName())) {
                    try {
                        methodClass = repository
                                .loadClass(ClassName.fromFieldSignatureToDottedClassName(methodC.getClassDescriptor().getSignature()));
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                    }
                }

                if (methodC != null && !Const.CONSTRUCTOR_NAME.equals(methodC.getName())
                        && !Const.STATIC_INITIALIZER_NAME.equals(methodC.getName())
                        && overridesSuperclassMethod(methodClass, methodC)) {
                    returnMethodUsedAsLock = methodC;
                    reportReturnValueBug(getThisClass(), getMethod(),
                            SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, getPC()));
                }

            }
        }
    }

    private void reportClassFieldBug(JavaClass jc, Method m, SourceLineAnnotation sla) {
        if (currentLockField != null && unsynchronizedMethods.contains(m)) {
            bugReporter.reportBug(
                    new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_FIELD", NORMAL_PRIORITY)
                            .addClassAndMethod(jc, m).addField(currentLockField).addSourceLine(sla));
        }
    }

    private void reportReturnValueBug(JavaClass jc, Method m, SourceLineAnnotation sla) {
        if (currentLockField == null) {
            bugReporter.reportBug(
                    new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_RETURN_VALUE", NORMAL_PRIORITY)
                            .addClassAndMethod(jc, m).addCalledMethod(returnMethodUsedAsLock).addSourceLine(sla));
            // How to access the method passed to addCalledMethod in the Tests?
        }
    }

    private void reportLocalVarBug(JavaClass jc, Method m, SourceLineAnnotation sla) {
        if (localVariableAnnotationsMap.containsKey(m)) {
            bugReporter.reportBug(
                    new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_LOCAL_VARIABLE", NORMAL_PRIORITY)
                            .addClassAndMethod(jc, m).add(localVariableAnnotationsMap.get(m)).addSourceLine(sla));
        }
    }

    @Override
    public void visitAfter(JavaClass jc) {
        isFirstVisit = false;
        currentPackageName = null;
        currentLockField = null;
        returnMethodUsedAsLock = null;
        unsynchronizedMethods.clear();
        localVariableAnnotationsMap.clear();
        threadSafeFields.clear();
        super.visitAfter(jc);
    }

    private LocalVariable getLockVariableFromStack(OpcodeStack stack) {
        if (stack.getStackDepth() <= 0) {
            return null;
        }

        Item topItem = stack.getStackItem(0);
        if (topItem == null) {
            return null;
        }

        if (topItem.isInitialParameter()) {
            return null;
        }

        int localVarIndex = topItem.getRegisterNumber();
        LocalVariableTable localVariableTable = getLocalVariableTableOfMethod();
        if (localVariableTable == null) {
            return null;
        }

        LocalVariable localVariable = localVariableTable.getLocalVariable(localVarIndex, getPC());
        return localVariable;
    }

    private boolean hasMethod(JavaClass javaClass, XMethod method) {
        Method[] methods = javaClass.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(method.getName()) && m.getSignature().equals(method.getSignature())) {
                return true;
            }
        }
        return false;
    }

    // Reference to the original method. Link:
    // https://github.com/spotbugs/spotbugs/blob/master/spotbugs/src/main/java/edu/umd/cs/findbugs/model/ClassFeatureSet.java#L125
    private boolean overridesSuperclassMethod(JavaClass javaClass, XMethod method) {
        if (method.isStatic() || javaClass == null) {
            return false;
        }

        boolean overrides = false;

        try {
            overrides = hasMethod(javaClass, method);

            JavaClass currentClass = javaClass.getSuperClass();
            while (currentClass != null && !currentClass.getClassName().equals("java.lang.Object")) {
                if (hasMethod(currentClass, method)) {
                    overrides = true;
                    break;
                }
                currentClass = currentClass.getSuperClass();
            }

            JavaClass[] interfaces = javaClass.getAllInterfaces();
            for (JavaClass interfaceClass : interfaces) {
                if (hasMethod(interfaceClass, method)) {
                    overrides = true;
                    break;
                }
            }

            return overrides;
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return true;
        }
    }
}
