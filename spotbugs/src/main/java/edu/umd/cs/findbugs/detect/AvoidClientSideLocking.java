/*
 * SpotBugs - Find bugs in Java programs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;

public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private boolean isFirstVisit = false;
    private XField currentLockField = null;
    private XMethod returnMethodUsedAsLock = null;
    private final Set<Method> unsynchronizedMethods = new HashSet<>();
    private final Map<XMethod, LocalVariableAnnotation> localVariableAnnotationsMap = new HashMap<>();
    private final Set<XField> threadSafeFields = new HashSet<>();

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /*
     * Visit the class and its methods. If the method is not synchronized, visit it.
     */
    @Override
    public void visit(JavaClass jc) {
        isFirstVisit = true;
        Method[] methods = jc.getMethods();
        for (Method obj : methods) {
            if (isNotConstructorOrStaticInitializer(obj.getName()) && !obj.isSynchronized()) {
                unsynchronizedMethods.add(obj);
            }
            doVisitMethod(obj);
        }

        isFirstVisit = false;
        super.visit(jc);
    }

    @Override
    public void sawOpcode(int seen) {
        /* If it is the first visit, check if any of the fields are thread safe. */
        if (isFirstVisit) {
            if ((seen == Const.PUTFIELD || seen == Const.PUTSTATIC) && Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                XField xfield = getXFieldOperand();
                if (xfield != null) {
                    Item value = stack.getStackItem(0);
                    XMethod returnValueOf = value.getReturnValueOf();
                    boolean isSignatureThreadSafe = value.getSignature() != null
                            && isThreadSafeField(value.getSignature());
                    boolean isReturnValueThreadSafe = returnValueOf != null
                            && isThreadSafeField(returnValueOf.getName());
                    if (isSignatureThreadSafe || isReturnValueThreadSafe) {
                        threadSafeFields.add(xfield);
                    }
                }
            }

            /*
             * If a synchronized block was entered, check if the lock is a field and if the
             * field is not thread safe.
             * Or if the lock is a local variable, save its annotation.
             */
            if (seen == Const.MONITORENTER && stack.getStackDepth() > 0) {
                Item topItem = stack.getStackItem(0);
                if (topItem.getXField() != null) {
                    XField xfield = topItem.getXField();
                    if (xfield.getClassName().equals(getThisClass().getClassName())
                            && !threadSafeFields.contains(xfield)) {
                        currentLockField = xfield;
                    }
                }
                unsynchronizedMethods.remove(getMethod());
                LocalVariableAnnotation localVariableAnnotation = LocalVariableAnnotation
                        .getLocalVariableAnnotation(getMethod(), stack.getStackItem(0), getPC());
                if (localVariableAnnotation != null) {
                    localVariableAnnotationsMap.put(getXMethod(), localVariableAnnotation);
                }
            }
        } else {
            detectLockingProblems(seen);
        }
    }

    private void detectLockingProblems(int seen) {
        /*
         * If the instruction is a field (static) access, check if it is not a
         * constructor or static initializer.
         * Then check if the field is not thread safe and if the method is not
         * synchronized. If so, report a bug.
         */
        if (seen == Const.PUTFIELD || seen == Const.GETFIELD || seen == Const.GETSTATIC || seen == Const.PUTSTATIC) {
            if (isNotConstructorOrStaticInitializer(getMethodName())) {
                if (getXFieldOperand() != null && currentLockField != null) {
                    XField xfield = getXFieldOperand();
                    boolean isMethodUnsynchronized = unsynchronizedMethods.contains(getMethod());
                    boolean isFieldNotNull = xfield != null;
                    boolean isFieldNotThreadSafe = !threadSafeFields.contains(xfield);
                    boolean isSamePackage = xfield != null && xfield.getPackageName() != null
                            && xfield.getPackageName().equals(getThisClass().getPackageName());
                    if (isMethodUnsynchronized && isFieldNotNull && isFieldNotThreadSafe && isSamePackage) {
                        reportClassFieldBug(getThisClass(), getMethod(),
                                SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                                        getPC()));
                    }

                }
            }
        }

        /*
         * If the instruction is a monitor enter, check if the lock is a local variable
         * and if the class
         * of the local variable is in the same package as the current class. If so,
         * report a bug.
         */
        if (seen == Const.MONITORENTER) {
            LocalVariable localVariable = getLockVariableFromStack(stack);
            if (localVariable != null) {
                String className = ClassName.fromFieldSignatureToDottedClassName(localVariable.getSignature());
                if (className != null) {
                    try {
                        JavaClass localVarClass = AnalysisContext.lookupSystemClass(className);
                        if (localVarClass.getPackageName().equals(getThisClass().getPackageName())) {
                            reportLocalVarBug(getThisClass(), getXMethod(),
                                    SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                                            getPC()));
                        }
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                    }
                }
            }

            /*
             * If the instruction is a monitor enter, check if the lock is a return value of
             * a method.
             * If a method comes from a superclass, report a bug.
             */
            if (stack.getStackDepth() > 0) {
                XMethod methodC = stack.getStackItem(0).getReturnValueOf();
                JavaClass methodClass = null;
                if (methodC != null) {
                    String className = ClassName
                            .fromFieldSignatureToDottedClassName(methodC.getClassDescriptor().getSignature());
                    if (isNotConstructorOrStaticInitializer(methodC.getName()) && className != null) {
                        try {
                            methodClass = AnalysisContext.lookupSystemClass(className);
                        } catch (ClassNotFoundException e) {
                            AnalysisContext.reportMissingClass(e);
                        }
                    }
                }

                if (isValidMethod(methodC, methodClass)) {
                    returnMethodUsedAsLock = methodC;
                    reportReturnValueBug(getThisClass(), getMethod(),
                            SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, getPC()));
                }

            }
        }
    }

    /*
     * Check if the method is not a constructor and not a static initializer and if it is
     * overriden in the superclass.
     */
    private boolean isValidMethod(XMethod xmethod, JavaClass methodClass) {
        return xmethod != null
                && isNotConstructorOrStaticInitializer(xmethod.getName())
                && overridesSuperclassMethod(methodClass, xmethod);
    }

    /* Check if the method is not a constructor and not a static initializer. */
    private boolean isNotConstructorOrStaticInitializer(String methodName) {
        return !Const.CONSTRUCTOR_NAME.equals(methodName)
                && !Const.STATIC_INITIALIZER_NAME.equals(methodName);
    }

    /* After visiting a class, clear all the fields. */
    @Override
    public void visitAfter(JavaClass jc) {
        isFirstVisit = false;
        currentLockField = null;
        returnMethodUsedAsLock = null;
        unsynchronizedMethods.clear();
        localVariableAnnotationsMap.clear();
        threadSafeFields.clear();
        super.visitAfter(jc);
    }

    /* Get the local variable table. */
    private LocalVariableTable getLocalVariableTableOfMethod() {
        if (stack != null && stack.getStackDepth() > 0) {
            return getMethod().getLocalVariableTable();
        }
        return null;
    }

    /* Retrieve a local variable from the stack. */
    private LocalVariable getLockVariableFromStack(OpcodeStack stack) {
        if (stack.getStackDepth() <= 0) {
            return null;
        }

        Item topItem = stack.getStackItem(0);

        if (topItem == null || topItem.isInitialParameter()) {
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

    /* Check if the field is thread safe. */
    private boolean isThreadSafeField(String signature) {
        Set<String> interestingCollectionMethodNames = new HashSet<>(Arrays.asList(
                "synchronizedCollection", "synchronizedSet", "synchronizedSortedSet",
                "synchronizedNavigableSet", "synchronizedList", "synchronizedMap",
                "synchronizedSortedMap", "synchronizedNavigableMap"));

        return signature.contains("Ljava/util/concurrent/") || interestingCollectionMethodNames.contains(signature);

    }

    /* Check if the class contains a given method. */
    private boolean hasMethod(JavaClass javaClass, XMethod method) {
        Method[] methods = javaClass.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(method.getName()) && m.getSignature().equals(method.getSignature())) {
                return true;
            }
        }
        return false;
    }

    /*
     * Reference to the original method. Link:
     * https://github.com/spotbugs/spotbugs/blob/master/spotbugs/src/main/java/edu/
     * umd/cs/findbugs/model/ClassFeatureSet.java#L125
     *
     * Check if the method overrides a method from a superclass or comes from an
     * interface.
     */
    private boolean overridesSuperclassMethod(JavaClass javaClass, XMethod method) {
        if (method.isStatic() || javaClass == null) {
            return false;
        }

        try {
            JavaClass currentClass = javaClass.getSuperClass();
            while (currentClass != null && !"java.lang.Object".equals(currentClass.getClassName())) {
                if (hasMethod(currentClass, method)) {
                    return true;
                }
                currentClass = currentClass.getSuperClass();
            }

            JavaClass[] interfaces = javaClass.getAllInterfaces();
            for (JavaClass interfaceClass : interfaces) {
                if (hasMethod(interfaceClass, method)) {
                    return true;
                }
            }

            return hasMethod(javaClass, method);
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }

    /* Report a bug if the lock is a field and the method is not synchronized. */
    private void reportClassFieldBug(JavaClass jc, Method m, SourceLineAnnotation sla) {
        if (currentLockField != null && unsynchronizedMethods.contains(m)) {
            bugReporter.reportBug(
                    new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_FIELD", NORMAL_PRIORITY)
                            .addClassAndMethod(jc, m).addField(currentLockField).addSourceLine(sla));
        }
    }

    /*
     * Report a bug if the lock is a return value of a method and the method comes
     * from a superclass.
     */
    private void reportReturnValueBug(JavaClass jc, Method m, SourceLineAnnotation sla) {
        if (currentLockField == null) {
            bugReporter.reportBug(
                    new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_RETURN_VALUE", NORMAL_PRIORITY)
                            .addClassAndMethod(jc, m).addCalledMethod(returnMethodUsedAsLock).addSourceLine(sla));
            // How to access the method passed to addCalledMethod in the Tests?
        }
    }

    /*
     * Report a bug if the lock is a local variable and the class of the local
     * variable is in the same package as the current class.
     */
    private void reportLocalVarBug(JavaClass jc, XMethod m, SourceLineAnnotation sla) {
        if (localVariableAnnotationsMap.containsKey(m)) {
            bugReporter.reportBug(
                    new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_LOCAL_VARIABLE", NORMAL_PRIORITY).addClassAndMethod(m).add(
                            localVariableAnnotationsMap.get(m)).addSourceLine(sla));
        }
    }
}
