package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCasesReferredToCourtActionGroups;

import org.junit.Test;

public class CasesReferredToCourtTest extends SjpDroolsAccessControlTest {

    public CasesReferredToCourtTest() {
        super("sjp.query.cases-referred-to-court", getQueryCasesReferredToCourtActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCasesReferredToCourt() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToQueryCasesReferredToCourt() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}