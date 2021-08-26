package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseActionGroups;

import org.junit.Test;

public class ProsecutionCaseQueryByIdTest extends SjpDroolsAccessControlTest {

    public ProsecutionCaseQueryByIdTest() {
        super("sjp.query.prosecution-case", getQueryCaseActionGroups());
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