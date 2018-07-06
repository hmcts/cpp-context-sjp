package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCasesSearchByMaterialIdActionGroups;

import org.junit.Test;

public class CaseSearchByMaterialIdTest extends SjpDroolsAccessControlTest {

    public CaseSearchByMaterialIdTest() {
        super("sjp.query.cases-search-by-material-id", getQueryCasesSearchByMaterialIdActionGroups());
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