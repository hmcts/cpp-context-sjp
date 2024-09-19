package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseByUrnPostcodeActionGroups;

import org.junit.jupiter.api.Test;

public class QueryCaseByUrnPostCodeTest extends SjpDroolsAccessControlTest {

    public QueryCaseByUrnPostCodeTest() {
        super("QUERY_API_SESSION", "sjp.query.case-by-urn-postcode", getQueryCaseByUrnPostcodeActionGroups());
    }

    @Test
    public void shouldAllowAuthorisedUserToIssueUrnPostcodeQuery() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToIssueUrnPostcodeQuery() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}