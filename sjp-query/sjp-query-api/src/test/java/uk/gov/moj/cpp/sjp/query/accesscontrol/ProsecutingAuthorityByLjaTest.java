package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQuerySessionGroups;

import org.junit.jupiter.api.Test;

public class ProsecutingAuthorityByLjaTest extends SjpDroolsAccessControlTest {

    public ProsecutingAuthorityByLjaTest() {
        super("QUERY_API_SESSION", "sjp.query.prosecuting-authority-for-lja", getQuerySessionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryProsecutingAuthorityForLja() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToQueryProsecutingAuthorityForLja() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}