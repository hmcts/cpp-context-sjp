package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getAllowedGroupsForTransparencyReport;

import org.junit.jupiter.api.Test;

public class TransparencyReportMetadataTest extends SjpDroolsAccessControlTest {

    public TransparencyReportMetadataTest() {
        super("QUERY_API_SESSION", "sjp.query.transparency-report-metadata", getAllowedGroupsForTransparencyReport());
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