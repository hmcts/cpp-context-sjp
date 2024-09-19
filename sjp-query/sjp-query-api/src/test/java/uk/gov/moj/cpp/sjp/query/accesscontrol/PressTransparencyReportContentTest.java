package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getAllowedGroupsForPressTransparencyReport;

import org.junit.jupiter.api.Test;

public class PressTransparencyReportContentTest extends SjpDroolsAccessControlTest {

    public PressTransparencyReportContentTest() {
        super("QUERY_API_SESSION", "sjp.query.press-transparency-report-content", getAllowedGroupsForPressTransparencyReport());
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