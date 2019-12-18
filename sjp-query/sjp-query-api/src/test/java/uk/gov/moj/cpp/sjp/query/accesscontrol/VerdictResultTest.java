package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getAllowedGroupsForVerdictCalculation;

import org.junit.Test;

public class VerdictResultTest extends SjpDroolsAccessControlTest {

    public VerdictResultTest() {
        super("sjp.query.offence-verdicts", getAllowedGroupsForVerdictCalculation());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryVerdicts(){
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToSearchCases() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}
