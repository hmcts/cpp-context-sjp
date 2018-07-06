package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryFindCaseSearchResultsActionGroups;

import org.junit.Test;

public class SearchResultsTest extends SjpDroolsAccessControlTest {

    public SearchResultsTest() {
        super("sjp.query.case-search-results", getQueryFindCaseSearchResultsActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToSearchCases() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToSearchCases() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}