package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryAwaitingCasesActionGroups;

import org.junit.Test;

public class AwaitingCasesTest extends SjpDroolsAccessControlTest {

    public AwaitingCasesTest() {
        super("sjp.query.awaiting-cases", getQueryAwaitingCasesActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryAwaitingCases() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToQueryAwaitingCases() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}