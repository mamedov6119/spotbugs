package edu.umd.cs.findbugs.detect;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;

public class AvoidClientSideLocking extends BytecodeScanningDetector {
    private final BugReporter bugReporter;

    public AvoidClientSideLocking(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitMethod(Method obj) {
        if (obj.getName().contains("фыв")) {
            bugReporter.reportBug(new BugInstance(this, "ACSL_AVOID_CLIENT_SIDE_LOCKING", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addString("Hello"));
        }
    }

}
