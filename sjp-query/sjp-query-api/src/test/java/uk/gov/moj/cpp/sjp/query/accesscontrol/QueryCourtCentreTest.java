package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCourtCentreGroups;

import org.junit.Test;


public class QueryCourtCentreTest extends SjpDroolsAccessControlTest {

    public QueryCourtCentreTest() {
        super("sjp.query.courtcentre", getQueryCourtCentreGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCourtCentre() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToQueryCourtCentre() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}
