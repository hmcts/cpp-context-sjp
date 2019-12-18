package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseActionGroups;

import org.junit.Test;

public class CaseQueryByIdWithDocumentMetadataTest extends SjpDroolsAccessControlTest {

    public CaseQueryByIdWithDocumentMetadataTest() {
        super("sjp.query.case-with-document-metadata", getQueryCaseActionGroups());
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

