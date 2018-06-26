package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseByUrnActionGroups;

import org.junit.Test;

public class QueryCaseByUrnTest extends SjpDroolsAccessControlTest {

    public QueryCaseByUrnTest() {
        super("sjp.query.case-by-urn", getQueryCaseByUrnActionGroups());
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