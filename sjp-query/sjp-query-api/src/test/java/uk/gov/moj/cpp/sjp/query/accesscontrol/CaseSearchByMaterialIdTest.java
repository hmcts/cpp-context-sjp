package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCasesSearchByMaterialIdActionGroups;

import org.junit.jupiter.api.Test;

public class CaseSearchByMaterialIdTest extends SjpDroolsAccessControlTest {

    public CaseSearchByMaterialIdTest() {
        super("QUERY_API_SESSION", "sjp.query.cases-search-by-material-id", getQueryCasesSearchByMaterialIdActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToSearchCasesByMaterialId() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToSearchCasesByMaterialId() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}