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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.util.SyntheticRepository;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;

public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private boolean isFirstVisit = false;
    private String currentPackageName = null;
    private XField currentLockField = null;
    private XMethod methodToAdd = null;
    private final Set<Method> methodsToReport = new HashSet<>();
    private final Set<Method> unsynchronizedMethods = new HashSet<>();
    private final Set<JavaClass> classesNotToReport = new HashSet<>();
    private final Set<Method> methodsLocalVarReport = new HashSet<>();
    private final Queue<LocalVariableAnnotation> localVariableAnnotationsQueue = new LinkedList<>();

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
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
            if (!Const.CONSTRUCTOR_NAME.equals(obj.getName()) && !Const.STATIC_INITIALIZER_NAME.equals(obj.getName())) {
                if (obj.isSynchronized()) {
                    getLocalVariableTableFromMethod();
                } else {
                    unsynchronizedMethods.add(obj);
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
                        currentLockField = stack.getStackItem(0).getXField();
                    }
                    if (unsynchronizedMethods.contains(getMethod())) {
                        unsynchronizedMethods.remove(getMethod());
                    }
                    getLocalVariableTableFromMethod();
                }
            }
        } else {
            if (stack.getStackDepth() > 0) {
                getLocalVariableTableFromMethod();
            }
            detectLockingProblems(seen);
        }
    }

    private LocalVariableTable getLocalVariableTableFromMethod() {
        if (stack != null && stack.getStackDepth() > 0) {
            return getMethod().getLocalVariableTable();
        }
        return null;
    }

    private void detectLockingProblems(int seen) {
        if (seen == Const.PUTFIELD || seen == Const.GETFIELD) {
            if (!Const.CONSTRUCTOR_NAME.equals(getMethodName())
                    && !Const.STATIC_INITIALIZER_NAME.equals(getMethodName())) {
                if (getXFieldOperand() != null && currentLockField != null) {
                    XField xfield = getXFieldOperand();
                    if (xfield.getName().equals(currentLockField.getName())
                            && unsynchronizedMethods.contains(getMethod())) {
                        methodsToReport.add(getMethod());
                    }
                }
            }
        }
        if (seen == Const.MONITORENTER) {
            if (getLocalVariableTableFromMethod() != null && getLockVariableFromStack(stack) != null) {
                org.apache.bcel.util.Repository repository = SyntheticRepository.getInstance();
                LocalVariable localVariable = getLockVariableFromStack(stack);
                JavaClass localVarClass = null;
                try {
                    repository.loadClass(ClassName
                            .fromFieldSignatureToDottedClassName(localVariable.getSignature()));
                    localVarClass = repository
                            .loadClass(ClassName.fromFieldSignatureToDottedClassName(localVariable.getSignature()));
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                } catch (NullPointerException e) {
                    AnalysisContext.logError("NullPointerException: " + e.getMessage(), e);
                }


                if (localVarClass != null && localVarClass.getPackageName().equals(currentPackageName)) {
                    methodsLocalVarReport.add(getMethod());
                }

            }
            if (stack.getStackDepth() > 0) {
                XMethod methodC = stack.getStackItem(0).getReturnValueOf();
                if (methodC != null && !Const.CONSTRUCTOR_NAME.equals(methodC.getName())
                        && !Const.STATIC_INITIALIZER_NAME.equals(methodC.getName())
                        && overridesSuperclassMethod(getThisClass(), methodC)) {
                    methodsToReport.add(getMethod());
                    methodToAdd = methodC;
                }
            }
        }
    }

    @Override
    public void visitAfter(JavaClass jc) {
        if (currentLockField != null) {
            for (Method method : methodsToReport) {
                bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_FIELD", NORMAL_PRIORITY)
                        .addClass(jc).addMethod(jc, method).addField(currentLockField));
            }
        }
        if (!classesNotToReport.contains(jc) && currentLockField == null) {
            for (Method method : methodsToReport) {
                bugReporter.reportBug(
                        new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_RETURN_VALUE", NORMAL_PRIORITY)
                                .addClass(jc).addMethod(jc, method).addMethod(methodToAdd));
            }
        }

        if (!methodsLocalVarReport.isEmpty()) {
            for (Method method : methodsLocalVarReport) {
                bugReporter.reportBug(
                        new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_LOCAL_VARIABLE", NORMAL_PRIORITY)
                                .addClass(jc).addMethod(jc, method).add(localVariableAnnotationsQueue.remove()));
            }
        }

        methodsToReport.clear();
        methodsLocalVarReport.clear();
        unsynchronizedMethods.clear();
        classesNotToReport.clear();
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
        LocalVariableTable localVariableTable = getLocalVariableTableFromMethod();
        if (localVariableTable == null || localVarIndex < 0) {
            return null;
        }

        for (LocalVariable lv : localVariableTable.getLocalVariableTable()) {
            if (lv.getIndex() == localVarIndex) {
                return lv;
            }
        }

        return null;
    }

    // Reference to the original method. Link:
    // https://gitlab.inf.elte.hu/java-static-analysis/spotbugs/-/merge_requests/20/diffs#66a21fadeee4760e4e500b0a7becbbce65e3af18
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
        for (Location location : cfg.orderedLocations()) {
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();
            int pc = handle.getPosition();
            boolean synchBlock = false;
            if (instruction instanceof MONITORENTER) {
                synchBlock = true;
            }
            if (instruction instanceof MONITOREXIT) {
                synchBlock = true;
            }
            if (instruction instanceof PUTFIELD) {
                OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), method,
                        handle.getPosition());
                OpcodeStack.Item stackItem = stack.getStackItem(0);
                if (isConcurrentOrSynchronizedField(stackItem.getReturnValueOf())) {
                    classesNotToReport.add(classContext.getJavaClass());
                }
            } else if (synchBlock) {
                OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), method,
                        handle.getPosition());
                if (stack.getStackDepth() > 0) {
                    LocalVariableAnnotation localVariableAnnotation = LocalVariableAnnotation.getLocalVariableAnnotation(method, stack.getStackItem(
                            0), pc);
                    if (localVariableAnnotation != null) {
                        localVariableAnnotationsQueue.add(localVariableAnnotation);
                    }
                }
            }
        }
    }

    // Reference to the original method. Link:
    // https://github.com/spotbugs/spotbugs/blob/master/spotbugs/src/main/java/edu/umd/cs/findbugs/model/ClassFeatureSet.java#L125
    private boolean overridesSuperclassMethod(JavaClass javaClass, XMethod method) {
        if (method.isStatic()) {
            return false;
        }
        try {
            JavaClass[] superClasses = javaClass.getSuperClasses();
            if (superClasses != null) {
                JavaClassAndMethod match = Hierarchy.findMethod(superClasses, method.getName(), method.getSignature(),
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
