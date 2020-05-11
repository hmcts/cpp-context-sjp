package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getAllowedGroupsForPressTransparencyReport;

import org.junit.Test;

public class PressTransparencyReportContentTest extends SjpDroolsAccessControlTest {

    public PressTransparencyReportContentTest() {
        super("sjp.query.press-transparency-report-content", getAllowedGroupsForPressTransparencyReport());
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