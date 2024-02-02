package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;


public class AvoidClientSideLocking extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private boolean isInsideSynchronizedBlock;
    private XField currentLockField;

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.isInsideSynchronizedBlock = false;
        this.currentLockField = null;
    }

    @Override
    public void visit(JavaClass obj) {
        isInsideSynchronizedBlock = false;
        currentLockField = null;
        super.visit(obj);
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            isInsideSynchronizedBlock = true;
            if (stack.getStackDepth() > 0) {
                Item top = stack.getStackItem(0);
                XField field = top.getXField();
                if (field != null) {
                    currentLockField = field;
                } 
            }
        } else if (seen == Const.MONITOREXIT) {
            isInsideSynchronizedBlock = false;
        } else if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
            if (isInsideSynchronizedBlock && currentLockField != null) {
                checkAndReportViolation(currentLockField);
            }
        }
    }


    private void checkAndReportViolation(XField lockField) {
        // implement logic to check for violations and then report bugs
        // compare lockField with the class's declared locking strategy
        // report a bug if the client-side locking is detected
        if (isInsideSynchronizedBlock && currentLockField != null) {
            bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addString("Avoid client-side locking when using classes that do not commit to their locking strategy"));
        }
    }

}
