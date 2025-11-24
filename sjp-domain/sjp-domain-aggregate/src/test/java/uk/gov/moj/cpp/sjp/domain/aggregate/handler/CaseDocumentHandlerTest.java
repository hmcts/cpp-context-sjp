package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploadRejected;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseDocumentHandlerTest {

    @Test
    public void uploadCaseDocument_whenCaseReferredForCourtHearing_shouldReturnCaseDocumentUploadRejectedEvent() {
        UUID caseId = UUID.randomUUID();
        UUID documentReference = UUID.randomUUID();
        String documentType = "type";
        CaseAggregateState state = mock(CaseAggregateState.class);

        when(state.hasGrantedApplication()).thenReturn(false);
        when(state.isCaseReferredForCourtHearing()).thenReturn(true);

        Stream<Object> result = CaseDocumentHandler.INSTANCE.uploadCaseDocument(caseId, documentReference, documentType, state);

        assertThat(result.collect(Collectors.toList()), contains(instanceOf(CaseDocumentUploadRejected.class)));
    }

    @Test
    public void uploadCaseDocument_whenCaseNotManagedByAtcm_shouldReturnCaseDocumentUploadRejectedEvent() {
        UUID caseId = UUID.randomUUID();
        UUID documentReference = UUID.randomUUID();
        String documentType = "type";
        CaseAggregateState state = mock(CaseAggregateState.class);

        when(state.hasGrantedApplication()).thenReturn(false);
        when(state.isCaseReferredForCourtHearing()).thenReturn(false);
        when(state.isManagedByAtcm()).thenReturn(false);

        Stream<Object> result = CaseDocumentHandler.INSTANCE.uploadCaseDocument(caseId, documentReference, documentType, state);

        assertThat(result.collect(Collectors.toList()), contains(instanceOf(CaseDocumentUploadRejected.class)));
    }

    @Test
    public void uploadCaseDocument_whenCaseManagedByAtcm_shouldReturnCaseDocumentUploadedEvent() {
        UUID caseId = UUID.randomUUID();
        UUID documentReference = UUID.randomUUID();
        String documentType = "type";
        CaseAggregateState state = mock(CaseAggregateState.class);

        when(state.hasGrantedApplication()).thenReturn(true);

        Stream<Object> result = CaseDocumentHandler.INSTANCE.uploadCaseDocument(caseId, documentReference, documentType, state);

        assertThat(result.collect(Collectors.toList()), contains(instanceOf(CaseDocumentUploaded.class)));
    }
}