package uk.gov.moj.cpp.sjp.query.accesscontrol.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryCaseDocumentsActionGroups;

import uk.gov.moj.cpp.sjp.query.accesscontrol.SjpDroolsAccessControlTest;

import org.junit.jupiter.api.Test;

public class CaseDocumentMetadataTest extends SjpDroolsAccessControlTest {

    public CaseDocumentMetadataTest() {
        super("QUERY_API_SESSION", "sjp.query.case-document-metadata", getQueryCaseDocumentsActionGroups());
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupAndWithProsecutingAuthorityToGetCaseDocumentMetadata() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        givenUserHasProsecutingAuthority();
        assertSuccessfulOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupAndWithoutProsecutingAuthorityToGetCaseDocumentMetadata() {
        givenUserIsMemberOfAnyOfTheSuppliedGroups();
        givenUserHasNotProsecutingAuthority();
        assertFailureOutcome(executeRules());
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToGetCaseDocumentMetadata() {
        givenUserIsNotMemberOfAnyOfTheSuppliedGroups();
        assertFailureOutcome(executeRules());

        verify(sjpProvider, never()).hasProsecutingAuthorityToCase(any());
    }
}