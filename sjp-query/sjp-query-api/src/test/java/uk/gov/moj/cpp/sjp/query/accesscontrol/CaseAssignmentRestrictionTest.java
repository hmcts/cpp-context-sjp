package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getAllowedGroupsForCaseAssignmentRestriction;

import org.junit.Test;

public class CaseAssignmentRestrictionTest extends SjpDroolsAccessControlTest {

    public CaseAssignmentRestrictionTest() {
        super("sjp.query.case-assignment-restriction", getAllowedGroupsForCaseAssignmentRestriction());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryFinancialMeans() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToQueryFinancialMeans() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}
