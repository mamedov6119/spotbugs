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

    @Test
    void testBadIPAddress1() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingIP.class",
                "avoidClientSideLocking/PrintableIPAddressList.class");
        assertReturnValueBug("addAndPrintIPAddresses", "PrintableIPAddressList");
        assertReturnValueBug("test", "PrintableIPAddressList");
        assertNumOfACSLBugs(ACSL_RETURN, 2);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadIPAddress2() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingIP2.class",
                "avoidClientSideLocking/PrintableIPAddressList2.class");
        assertReturnValueBug("addAndPrintIPAddresses", "PrintableIPAddressList2");
        assertNumOfACSLBugs(ACSL_RETURN, 1);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadBookClass1() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("getDueDate", "BadClientSideLockingBook");
        assertFieldBug("issue", "BadClientSideLockingBook");
        assertNumOfACSLBugs(ACSL_FIELD, 2);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadBookClass2() {
        performAnalysis("avoidClientSideLocking/BadExampleBook.class", "avoidClientSideLocking/Book.class");
        assertLocalVariableBug("renew", "BadExampleBook");
        assertLocalVariableBug("testing", "BadExampleBook");
        assertNumOfACSLBugs(ACSL_LOCAL, 2);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
    }

    @Test
    void testBadBookClass3() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBookC.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("issue", "BadClientSideLockingBookC");
        assertNumOfACSLBugs(ACSL_FIELD, 1);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadBookClass4() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBookS.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("getDueDate", "BadClientSideLockingBookS");
        assertNumOfACSLBugs(ACSL_FIELD, 1);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadBookClass5() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingExample1.class",
                "avoidClientSideLocking/Book.class");
        assertLocalVariableBug("testing", "BadClientSideLockingExample1");
        assertNumOfACSLBugs(ACSL_LOCAL, 1);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
    }

    @Test
    void testGoodLocking() {
        performAnalysis("avoidClientSideLocking/GoodClientSideLockingBook.class", "avoidClientSideLocking/Book.class",
                "avoidClientSideLocking/GoodClientSideLockingIP.class", "avoidClientSideLocking/IPAddressList.class",
                "avoidClientSideLocking/Repository.class", "avoidClientSideLocking/Repository$1.class",
                "avoidClientSideLocking/Repository$RepositoryDateFormat.class");
        assertZeroACSLBugs("GoodClientSideLockingBook");
        assertZeroACSLBugs("Book");
        assertZeroACSLBugs("GoodClientSideLockingIP");
        assertZeroACSLBugs("IPAddressList");
        assertZeroACSLBugs("Repository");
        assertZeroACSLBugs("Repository$1");
        assertZeroACSLBugs("Repository$RepositoryDateFormat");
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

    private void assertZeroACSLBugs(String className) {
        final String[] bugTypes = new String[] { ACSL_FIELD, ACSL_RETURN, ACSL_LOCAL };
        for (String bugType : bugTypes) {
            assertNumOfACSLBugs(bugType, 0);
        }
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
