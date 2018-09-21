package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getReadyCasesReasonsCountsGroups;

import org.junit.Test;

public class ReadyCasesReasonsQueryTest extends SjpDroolsAccessControlTest {

    public ReadyCasesReasonsQueryTest() {
        super("sjp.query.ready-cases-reasons-counts", getReadyCasesReasonsCountsGroups());
    }

    @Test
    public void shouldAllowReadyCasesReasonsCountsQuery() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowReadyCasesReasonsCountsQuery() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());
    }
}