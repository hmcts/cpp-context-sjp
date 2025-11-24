package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseActionGroups;

import org.junit.jupiter.api.Test;


public class QueryCaseWithDocumentMetadataTest extends SjpDroolsAccessControlTest {

    public QueryCaseWithDocumentMetadataTest() {
        super("QUERY_API_SESSION", "sjp.query.case-with-document-metadata", getQueryCaseActionGroups());
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