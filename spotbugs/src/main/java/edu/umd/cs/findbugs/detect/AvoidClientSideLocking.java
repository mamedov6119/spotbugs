package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.XField;

public class AvoidClientSideLocking extends BytecodeScanningDetector {

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
            currentLockField = getLockField();
        } else if (seen == Const.MONITOREXIT) {

        } else if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
            if (isInsideSynchronizedBlock && currentLockField != null) {
                // Violations and report bugs
                checkAndReportViolation(currentLockField);
            }
        }
    }

    private XField getLockField() {
        // Logic to get the field related to the lock
        return null;
    }

    private void checkAndReportViolation(XField lockField) {
        // Implement logic to check for violations and report bugs
        // Compare lockField with the class's declared locking strategy
        // Report a bug if the client-side locking is detected

        bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY)
                .addClassAndMethod(this)
                .addString("Avoid client-side locking when using classes that do not commit to their locking strategy"));
    }

}
