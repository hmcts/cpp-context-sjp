package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseActionGroups;

import org.junit.jupiter.api.Test;

public class CaseQueryByIdTest extends SjpDroolsAccessControlTest {

    public CaseQueryByIdTest() {
        super("QUERY_API_SESSION","sjp.query.case", getQueryCaseActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCase() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToQueryCase() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}