package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getAllowedGroupsForCasesWithoutDefendantPostcode;

import org.junit.Test;

public class CasesWithoutDefendantPostcodeTest extends SjpDroolsAccessControlTest {

    public CasesWithoutDefendantPostcodeTest() {
        super("sjp.query.cases-without-defendant-postcode", getAllowedGroupsForCasesWithoutDefendantPostcode());
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