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
import java.util.HashSet;
import java.util.Set;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.util.SyntheticRepository;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.XFactory;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import org.apache.bcel.classfile.LocalVariable;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;

public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private final Set<Method> methodsToReport;
    private final Set<String> unsynchronizedMethods;
    private final Set<XField> concurrentOrSynchronizedFields;
    private String currentLockFieldName;
    private LocalVariableTable localVariableTable;
    private final HashSet<JavaClass> classesNotToReport;
    private final HashSet<Method> methodsLocalVarReport;
    private final HashSet<JavaClass> classesToCheck;
    private boolean isFirstVisit;
    private static org.apache.bcel.util.Repository repository = SyntheticRepository.getInstance();
    private static String currentPackageName;

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.methodsToReport = new HashSet<>();
        this.localVariableTable = null;
        this.currentLockFieldName = null;
        this.concurrentOrSynchronizedFields = new HashSet<>();
        this.classesNotToReport = new HashSet<>();
        this.methodsLocalVarReport = new HashSet<>();
        this.unsynchronizedMethods = new HashSet<>();
        this.classesToCheck = new HashSet<>();
    }

    @Override
    public void visit(JavaClass jc) {
        currentPackageName = jc.getPackageName();
        isFirstVisit = true;
        Method[] methods = jc.getMethods();
        for (Method obj : methods) {
            try {
                collectConcurrentOrSynchronizedFieldsAndClassesNotToReport(getClassContext(), obj);
            } catch (CFGBuilderException e) {
                AnalysisContext.logError("CFGBuilderException: " + e.getMessage(), e);
            }
            if (!Const.CONSTRUCTOR_NAME.equals(obj.getName())) {
                if (obj.isSynchronized()) {
                    localVariableTable = obj.getLocalVariableTable();
                } else {
                    unsynchronizedMethods.add(obj.getName());
                    doVisitMethod(obj);
                }
            }
        }

        isFirstVisit = false;
        super.visit(jc);
    }

    @Override
    public void sawOpcode(int seen) {
        if (isFirstVisit) {
            if (seen == Const.MONITORENTER) {
                if (stack.getStackDepth() > 0) {
                    if (stack.getStackItem(0).getXField() != null) {
                        currentLockFieldName = stack.getStackItem(0).getXField().getName();
                    }
                    if (!unsynchronizedMethods.isEmpty() && unsynchronizedMethods.contains(getMethodName())) {
                        unsynchronizedMethods.remove(getMethodName());
                    }
                    getLocalVariableTable();
                }
            }
        } else {
            if (stack.getStackDepth() > 0) {
                getLocalVariableTable();
            }
            reportMainBugs(seen);
        }
    }

    private void getLocalVariableTable() {
        if (stack.getStackDepth() > 0) {
            localVariableTable = getMethod().getLocalVariableTable();
        }
    }

    private void reportMainBugs(int seen) {
        if (seen == Const.PUTFIELD || seen == Const.GETFIELD) {
            if (!Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                if (getXFieldOperand() != null) {
                    String fieldName = getXFieldOperand().getName();
                    if (currentLockFieldName != null && fieldName.equals(currentLockFieldName)
                            && unsynchronizedMethods.contains(getMethodName())) {
                        methodsToReport.add(getMethod());
                    }
                }
            }
        }
        if (seen == Const.MONITORENTER) {
            if (localVariableTable != null && getLockVariableFromStack(stack) != null && stack.getStackDepth() > 0) {
                LocalVariable localVar = getLockVariableFromStack(stack);
                try {
                    repository.loadClass(ClassName
                            .fromFieldSignatureToDottedClassName(localVar.getSignature()));
                } catch (ClassNotFoundException e) {
                    AnalysisContext.logError("Could not load a class: " + e.getMessage(), e);
                }
                JavaClass localVarClass = repository
                        .findClass(ClassName.fromFieldSignatureToDottedClassName(localVar
                                .getSignature()));
                if (localVarClass.getPackageName().equals(currentPackageName)) {
                    classesToCheck.add(localVarClass);
                    methodsLocalVarReport.add(getMethod());
                }

            }
            if (stack.getStackDepth() > 0) {
                OpcodeStack.Item top = stack.getStackItem(0);
                XMethod methodC = top.getReturnValueOf();
                if (methodC != null && !Const.CONSTRUCTOR_NAME.equals(methodC.getName())
                        && overridesSuperclassMethod(getThisClass(), methodC)) {
                    methodsToReport.add(getMethod());
                }
            }
        }
    }

    @Override
    public void visitAfter(JavaClass jc) {
        if (currentLockFieldName != null) {
            for (Method method : methodsToReport) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_FIELD", NORMAL_PRIORITY)
                        .addClass(jc).addMethod(jc, method)
                        .addString(
                                "All methods must be synchronized when using the field as a lock object in a synchronized block"));
            }
        }
        if (!classesNotToReport.contains(jc)) {
            for (Method method : methodsToReport) {
                bugReporter.reportBug(
                        new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_RETURN_VALUE", NORMAL_PRIORITY)
                                .addClass(jc).addMethod(jc, method)
                                .addString("Do not use a return value as a lock object in a synchronized block"));
            }
        }

        if (!methodsLocalVarReport.isEmpty()) {
            for (Method method : methodsLocalVarReport) {
                bugReporter.reportBug(
                        new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_LOCAL_VARIABLE", NORMAL_PRIORITY)
                                .addClass(jc).addMethod(jc, method)
                                .addString("Local variable used as lock"));
            }
        }

        methodsToReport.clear();
        methodsLocalVarReport.clear();
        unsynchronizedMethods.clear();
        concurrentOrSynchronizedFields.clear();
        classesNotToReport.clear();
        super.visitAfter(jc);
    }

    private LocalVariable getLockVariableFromStack(OpcodeStack stack) {
        if (stack.getStackDepth() > 0) {
            Item topItem = stack.getStackItem(0);
            if (topItem != null && topItem.isInitialParameter()) {
                return null;
            }
            int localVarIndex = topItem.getRegisterNumber();
            if (localVariableTable != null && localVarIndex >= 0) {
                for (LocalVariable lv : localVariableTable.getLocalVariableTable()) {
                    if (lv.getIndex() == localVarIndex) {
                        return lv;
                    }
                }
            }
        }
        return null;
    }

    // Reference the original method.
    private static boolean isConcurrentOrSynchronizedField(ClassMember classMember) {
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

    private void collectConcurrentOrSynchronizedFieldsAndClassesNotToReport(ClassContext classContext, Method method)
            throws CFGBuilderException {
        CFG cfg = classContext.getCFG(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        for (Location location : cfg.orderedLocations()) {
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();

            if (instruction instanceof PUTFIELD) {
                OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), method,
                        handle.getPosition());
                OpcodeStack.Item stackItem = stack.getStackItem(0);
                if (isConcurrentOrSynchronizedField(stackItem.getReturnValueOf())) {
                    concurrentOrSynchronizedFields.add(XFactory.createXField((FieldInstruction) instruction, cpg));
                    classesNotToReport.add(classContext.getJavaClass());
                }
            }
        }
    }

    // reference original method
    private boolean overridesSuperclassMethod(JavaClass javaClass, XMethod method) {
        if (method.isStatic()) {
            return false;
        }

        try {
            JavaClass[] superclassList = javaClass.getSuperClasses();
            if (superclassList != null) {
                JavaClassAndMethod match = Hierarchy.findMethod(superclassList, method.getName(), method.getSignature(),
                        Hierarchy.INSTANCE_METHOD);
                if (match != null) {
                    return true;
                }
            }
            JavaClass[] interfaceList = javaClass.getAllInterfaces();
            if (interfaceList != null) {
                JavaClassAndMethod match = Hierarchy.findMethod(interfaceList, method.getName(), method.getSignature(),
                        Hierarchy.INSTANCE_METHOD);
                if (match != null) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
