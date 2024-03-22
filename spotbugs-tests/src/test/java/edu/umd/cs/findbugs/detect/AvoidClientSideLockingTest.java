package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class AvoidClientSideLockingTest extends AbstractIntegrationTest {

    @Test
    void testBadClientSideLocking() {
        // performAnalysis("avoidClientSideLocking/BadClientSideLockingBook.class", "avoidClientSideLocking/PrintableIPAddressList.class",
        //         "avoidClientSideLocking/DataUpdater.class", "avoidClientSideLocking/Book.class",
        //         "avoidClientSideLocking/BadClientSideLockingIP.class", "avoidClientSideLocking/BadClientSideLockingMap.class",
        //         "avoidClientSideLocking/BadClientSideLockingExample.class", "avoidClientSideLocking/BadExampleBook.class");

        // performAnalysis("avoidClientSideLocking/PrintableIPAddressList.class", "avoidClientSideLocking/BadClientSideLockingIP.class", "avoidClientSideLocking/BadExampleBook.class", "avoidClientSideLocking/Book.class");
        // performAnalysis("avoidClientSideLocking/BadClientSideLockingBookC.class", "avoidClientSideLocking/Book.class", "avoidClientSideLocking/PrintableIPAddressList.class", "avoidClientSideLocking/BadClientSideLockingIP.class");
        // performAnalysis("avoidClientSideLocking/BadClientSideLockingBook.class", "avoidClientSideLocking/Book.class");

        // performAnalysis("avoidClientSideLocking/PrintableIPAddressList.class", "avoidClientSideLocking/BadClientSideLockingIP.class");
        // performAnalysis("avoidClientSideLocking/BadExampleBook.class", "avoidClientSideLocking/Book.class");
        performAnalysis("avoidClientSideLocking/SpotBugsG.class");

        // assertACSLBug("addAndPrintIPAddresses", "PrintableIPAddressList");
        // assertACSLBugInMultipleMethods(List.of("getDueDate", "issue"), "BadClientSideLockingBook");
        // assertACSLBug("someMethod", "BadClientSideLockingExample");
        // assertACSLBug("issue", "BadClientSideLockingBookC");
        // assertACSLBug("renew", "BadExampleBook");
        assertACSLBug("getMnemonic", "SpotBugsG");

        assertNumOfACSLBugs(1);
    } //java, what is static analysis tools, spotbugs how works, other static analyzers, the problem I am solving

    @Test
    void testGoodEndOfFileChecks() {
        // performAnalysis("avoidClientSideLocking/GoodClientSideLockingBook.class", "avoidClientSideLocking/GoodClientSideLockingIP.class",
        //         "avoidClientSideLocking/DataUpdaterr.class", "avoidClientSideLocking/Book.class", "avoidClientSideLocking/IPAddressList.class",
        //         "avoidClientSideLocking/GoodClientSideLockingMap.class");
        performAnalysis("avoidClientSideLocking/GoodClientSideLockingBook.class", "avoidClientSideLocking/Book.class",
                "avoidClientSideLocking/GoodClientSideLockingIP.class", "avoidClientSideLocking/IPAddressList.class");
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
        System.out.println("ACSL_AVOID_CLIENT_SIDE_LOCKING bug was caught in " + className + "." + method + " method");
    }

    private void assertACSLBugInMultipleMethods(List<String> methods, String className) {
        for (String method : methods) {
            assertACSLBug(method, className);
        }
    }

}
