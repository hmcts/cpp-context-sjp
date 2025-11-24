package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseByUrnActionGroups;

import org.junit.jupiter.api.Test;

public class QueryCaseByUrnTest extends SjpDroolsAccessControlTest {

    public QueryCaseByUrnTest() {
        super("QUERY_API_SESSION", "sjp.query.case-by-urn", getQueryCaseByUrnActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCaseByUrn() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToQueryCaseByUrn() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}