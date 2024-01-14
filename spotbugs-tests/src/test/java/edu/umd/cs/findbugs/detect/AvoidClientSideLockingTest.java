package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class AvoidClientSideLockingTest extends AbstractIntegrationTest {

    @Test
    void testBadClientSideLocking() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook.class", "avoidClientSideLocking/PrintableIPAddressList.class",
                "avoidClientSideLocking/DataUpdater.class", "avoidClientSideLocking/Book.class",
                "avoidClientSideLocking/BadClientSideLockingIP.class", "avoidClientSideLocking/BadClientSideLockingMap.class");


        // assertACSLBug("фыв", "BadClientSideLockingBook");
        // assertACSLBug("фыв", "PrintableIPAddressList");
        // assertACSLBug("фыв", "DataUpdater");

        assertNumOfACSLBugs(0);
    }

    @Test
    void testGoodEndOfFileChecks() {
        performAnalysis("avoidClientSideLocking/GoodClientSideLockingBook.class", "avoidClientSideLocking/GoodClientSideLockingIP.class",
                "avoidClientSideLocking/DataUpdaterr.class", "avoidClientSideLocking/Book.class", "avoidClientSideLocking/IPAddressList.class",
                "avoidClientSideLocking/GoodClientSideLockingMap.class");

        assertNumOfACSLBugs(0);
    }

    private void assertNumOfACSLBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("ACSL_AVOID_CLIENT_SIDE_LOCKING").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertACSLBug(String method, String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("ACSL_AVOID_CLIENT_SIDE_LOCKING")
                .inClass(className)
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
