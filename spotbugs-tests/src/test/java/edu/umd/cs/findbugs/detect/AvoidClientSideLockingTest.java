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

    // @Test
    // void testBadMapClass1() {
    //     performAnalysis("avoidClientSideLocking/SpotBugsG.class");
    //     assertACSLBug("getMnemonic", "SpotBugsG");
    //     assertNumOfACSLBugs(1);
    // }

    // @Test
    // void testBadIPAddress1() {
    //     performAnalysis("avoidClientSideLocking/BadClientSideLockingIP.class", "avoidClientSideLocking/IPAddressList.class");
    //     assertACSLBug("addAndPrintIPAddresses", "BadClientSideLockingIP");
    //     assertNumOfACSLBugs(1);
    // }

    // @Test
    // void testBadBookClass1() {
    //     performAnalysis("avoidClientSideLocking/BadClientSideLockingBook.class", "avoidClientSideLocking/Book.class");
    //     assertACSLBugInMultipleMethods(List.of("getDueDate", "issue"), "BadClientSideLockingBook");
    //     assertNumOfACSLBugs(2);
    // }

    // @Test
    // void testBadBookClass2() {
    //     performAnalysis("avoidClientSideLocking/BadExampleBook.class", "avoidClientSideLocking/Book.class");
    //     assertACSLBug("renew", "BadExampleBook");
    //     assertNumOfACSLBugs(1);
    // }

    // @Test
    // void testBadBookClass3() {
    //     performAnalysis("avoidClientSideLocking/BadClientSideLockingBookC.class", "avoidClientSideLocking/Book.class");
    //     assertACSLBug("issue", "BadClientSideLockingBookC");
    //     assertNumOfACSLBugs(1);
    // }

    @Test
    void testBadBookClass4() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBookS.class", "avoidClientSideLocking/Book.class");
        assertACSLBug("getDueDate", "BadClientSideLockingBookS");
        assertNumOfACSLBugs(1);
    }

    // @Test
    // void testGoodEndOfFileChecks() {
    //     performAnalysis("avoidClientSideLocking/GoodClientSideLockingBook.class", "avoidClientSideLocking/Book.class",
    //             "avoidClientSideLocking/GoodClientSideLockingIP.class", "avoidClientSideLocking/IPAddressList.class");
    //     assertNumOfACSLBugs(0);
    // }

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
