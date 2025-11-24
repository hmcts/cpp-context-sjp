package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getCaseNotesGroups;

import org.junit.jupiter.api.Test;

public class QueryCaseNotesTest extends SjpDroolsAccessControlTest {

    public QueryCaseNotesTest() {
        super("QUERY_API_SESSION", "sjp.query.case-notes", getCaseNotesGroups());
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