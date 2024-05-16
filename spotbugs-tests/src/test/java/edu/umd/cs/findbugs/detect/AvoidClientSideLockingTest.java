package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;

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
        performAnalysis("avoidClientSideLocking/BadClientSideLockingIP1.class",
                "avoidClientSideLocking/PrintableIPAddressList.class");
        assertReturnValueBug("addAndPrintIPAddresses", "PrintableIPAddressList", 27, "addAndPrintIPAddresses");
        assertReturnValueBug("test", "PrintableIPAddressList", 36, "test");
        assertNumOfACSLBugs(ACSL_RETURN, 2);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);

    }

    @Test
    void testBadIPAddress2() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingIP2.class",
                "avoidClientSideLocking/PrintableIPAddressList2.class");
        assertReturnValueBug("addAndPrintIPAddresses", "PrintableIPAddressList2", 27, "addAndPrintIPAddresses");
        assertNumOfACSLBugs(ACSL_RETURN, 1);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadBookClass1() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook1.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("getDueDate", "BadClientSideLockingBook1", 19, "book");
        assertFieldBug("issue", "BadClientSideLockingBook1", 15, "book");
        assertNumOfACSLBugs(ACSL_FIELD, 2);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadBookClass2() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook2.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("issue", "BadClientSideLockingBook2", 14, "book");
        assertNumOfACSLBugs(ACSL_FIELD, 1);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

    @Test
    void testBadBookClass3() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook3.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("getDueDate", "BadClientSideLockingBook3", 21, "book");
        assertNumOfACSLBugs(ACSL_FIELD, 1);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }


    @Test
    void testBadBookClass4() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook4.class",
                "avoidClientSideLocking/Book.class", "avoidClientSideLocking/SynchObj.class");
        assertLocalVariableBug("testing", "BadClientSideLockingBook4", 33, "LocalSynchVar");
        assertNumOfACSLBugs(ACSL_LOCAL, 1);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
    }

    @Test
    void testBadBookClass5() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook5.class", "avoidClientSideLocking/Book.class",
                "avoidClientSideLocking/SynchObj.class");
        assertLocalVariableBug("renew", "BadClientSideLockingBook5", 24, "AUE");
        assertLocalVariableBug("testing", "BadClientSideLockingBook5", 35, "aaaa");
        assertNumOfACSLBugs(ACSL_LOCAL, 2);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
    }

    @Test
    void testBadBookClass6() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingBook6.class", "avoidClientSideLocking/Book.class");
        assertFieldBug("getDueDate", "BadClientSideLockingBook6", 21, "lock");
        assertNumOfACSLBugs(ACSL_FIELD, 1);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
    }

    @Test
    void testBadMapClass1() {
        performAnalysis("avoidClientSideLocking/BadClientSideLockingMap1.class",
                "avoidClientSideLocking/DataUpdater.class");
        assertReturnValueBug("updateAndPrintData", "DataUpdater", 21, "updateAndPrintData");
        assertNumOfACSLBugs(ACSL_RETURN, 1);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
    }

    @Test
    void testBadMapClass2() {
        performAnalysis("avoidClientSideLocking/TestCodeChecker.class");
        // assertReturnValueBug("updateAndPrintData", "DataUpdater", 21, "updateAndPrintData");
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
        assertNumOfACSLBugs(ACSL_FIELD, 0);
    }

    @Test
    void testGoodLocking() {
        performAnalysis("avoidClientSideLocking/GoodClientSideLockingBook1.class",
                "avoidClientSideLocking/GoodClientSideLockingBook2.class", "avoidClientSideLocking/Book.class",
                "avoidClientSideLocking/GoodClientSideLockingIP1.class", "avoidClientSideLocking/IPAddressList.class",
                "avoidClientSideLocking/GoodRepository1.class", "avoidClientSideLocking/GoodRepository1$1.class",
                "avoidClientSideLocking/GoodRepository1$RepositoryDateFormat.class",
                "avoidClientSideLocking/GoodClientSideLockingMap1.class");
        assertZeroACSLBugs();
    }

    private void assertReturnValueBug(String method, String className, int line, String methodName) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(ACSL_RETURN).inClass(className).inMethod(method).inMethod(methodName).atLine(line).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertFieldBug(String method, String className, int line, String fieldName) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(ACSL_FIELD).inClass(className).inMethod(method).atField(fieldName).atLine(line).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertLocalVariableBug(String method, String className, int line, String localVariableName) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(ACSL_LOCAL).inClass(className).inMethod(method).atVariable(localVariableName).atLine(line).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertNumOfACSLBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertZeroACSLBugs() {
        assertNumOfACSLBugs(ACSL_FIELD, 0);
        assertNumOfACSLBugs(ACSL_RETURN, 0);
        assertNumOfACSLBugs(ACSL_LOCAL, 0);
    }

}
