package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class AvoidClientSideLockingTest extends AbstractIntegrationTest {


    private static final String ACSL_RETURN = "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_RETURN_VALUE";
    private static final String ACSL_FIELD = "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_FIELD";
    private static final String ACSL_LOCAL = "ACSL_AVOID_CLIENT_SIDE_LOCKING_ON_LOCAL_VARIABLE";
    private static final String NO_BUG = "NO_BUG";

    @Test
    void testBadIPAddress1() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingIP.class", "avoidClientSideLocking/PrintableIPAddressList.class");
        assertACSLBugInMultipleMethods(List.of("addAndPrintIPAddresses", "test"), "PrintableIPAddressList", ACSL_RETURN);
        assertNumOfACSLBugs(ACSL_RETURN, 2);
    }

    @Test
    void testBadIPAddress2() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingIP2.class", "avoidClientSideLocking/PrintableIPAddressList2.class");
        assertReturnValueBug("addAndPrintIPAddresses", "PrintableIPAddressList2");
        assertNumOfACSLBugs(ACSL_RETURN, 1);
    }

    @Test
    void testBadBookClass1() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook.class", "avoidClientSideLocking/Book.class");
        assertACSLBugInMultipleMethods(List.of("getDueDate", "issue"), "BadClientSideLockingBook", ACSL_FIELD);
        assertNumOfACSLBugs(ACSL_FIELD, 2);
    }

    @Test
    void testBadBookClass2() {
        performAnalysis("avoidClientSideLocking/BadExampleBook.class", "avoidClientSideLocking/Book.class");
        assertACSLBugInMultipleMethods(List.of("renew", "testing"), "BadExampleBook", ACSL_LOCAL);
        assertNumOfACSLBugs(ACSL_LOCAL, 2);
    }

    @Test
    void testBadBookClass3() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBookC.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("issue", "BadClientSideLockingBookC");
        assertNumOfACSLBugs(ACSL_FIELD, 1);
    }

    @Test
    void testBadBookClass4() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBookS.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("getDueDate", "BadClientSideLockingBookS");
        assertNumOfACSLBugs(ACSL_FIELD, 1);
    }

    @Test
    void testBadBookClass5() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingExample1.class", "avoidClientSideLocking/Book.class");
        assertACSLBugInMultipleMethods(List.of("renew", "testing", "testing2"), "BadClientSideLockingExample1", ACSL_LOCAL);
        assertNumOfACSLBugs(ACSL_LOCAL, 3);
    }

    @Test
    void testGoodLocking() {
        performAnalysis("avoidClientSideLocking/GoodClientSideLockingBook.class", "avoidClientSideLocking/Book.class",
                "avoidClientSideLocking/GoodClientSideLockingIP.class", "avoidClientSideLocking/IPAddressList.class");
        assertNumOfACSLBugs(NO_BUG, 0);
    }



    private void assertReturnValueBug(String method, String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(ACSL_RETURN).inClass(className).inMethod(method).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertFieldBug(String method, String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(ACSL_FIELD).inClass(className).inMethod(method).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertLocalVariableBug(String method, String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(ACSL_LOCAL).inClass(className).inMethod(method).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertNumOfACSLBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertACSLBugInMultipleMethods(List<String> methods, String className, String bugType) {
        switch (bugType) {
        case ACSL_FIELD:
            for (String method : methods) {
                assertFieldBug(method, className);
            }
            break;
        case ACSL_RETURN:
            for (String method : methods) {
                assertReturnValueBug(method, className);
            }
            break;
        case ACSL_LOCAL:
            for (String method : methods) {
                assertLocalVariableBug(method, className);
            }
            break;
        default:
            break;
        }
    }

}
