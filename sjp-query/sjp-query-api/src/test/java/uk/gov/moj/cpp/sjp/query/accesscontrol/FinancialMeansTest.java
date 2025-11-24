package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryFinancialMeansActionGroups;

import org.junit.jupiter.api.Test;

public class FinancialMeansTest extends SjpDroolsAccessControlTest {

    public FinancialMeansTest() {
        super("QUERY_API_SESSION", "sjp.query.financial-means", getQueryFinancialMeansActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryFinancialMeans() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToQueryFinancialMeans() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}