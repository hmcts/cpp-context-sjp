package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryEmployerActionGroups;

import org.junit.jupiter.api.Test;

public class EmployerTest extends SjpDroolsAccessControlTest {

    public EmployerTest() {
        super("QUERY_API_SESSION", "sjp.query.employer", getQueryEmployerActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryEmployer() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToQueryEmployer() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}