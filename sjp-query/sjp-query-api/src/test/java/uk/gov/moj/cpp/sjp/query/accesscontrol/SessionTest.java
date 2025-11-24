package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQuerySessionGroups;

import org.junit.jupiter.api.Test;

public class SessionTest extends SjpDroolsAccessControlTest {

    public SessionTest() {
        super("QUERY_API_SESSION", "sjp.query.session", getQuerySessionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQuerySession() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToQuerySession() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}