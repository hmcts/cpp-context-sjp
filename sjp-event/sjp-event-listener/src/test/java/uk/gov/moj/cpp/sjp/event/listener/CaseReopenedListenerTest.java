package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseReopenedListenerTest {

    private static final String EVENTS_MARK_CASE_REOPENED = "sjp.events.case-reopened-in-libra";
    private static final String EVENTS_UPDATE_CASE_REOPENED = "sjp.events.case-reopened-in-libra-updated";
    private static final String EVENTS_UNDO_CASE_REOPENED = "sjp.events.case-reopened-in-libra-undone";

    private static final String METHOD_MARK_CASE_REOPENED = "markCaseReopened";
    private static final String METHOD_UPDATE_CASE_REOPENED = "updateCaseReopened";
    private static final String METHOD_UNDO_CASE_REOPENED = "undoCaseReopened";

    private static final CaseReopenDetails MARK_CASE_REOPEN_DETAILS = new CaseReopenDetails(
            UUID.randomUUID(), LocalDate.of(2016, 2, 2), "LIBRA12345", "Mandatory Reason");
    private static final CaseReopenDetails UPDATE_CASE_REOPEN_DETAILS = new CaseReopenDetails(
            MARK_CASE_REOPEN_DETAILS.getCaseId(), LocalDate.of(2016, 9, 10), "LIBRA13579", "Some Reason");


    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private CaseUpdatedListener listener;

    @Mock
    private CaseDetail caseDetails;

    @Test
    public void shouldHandleMarkCaseReopenedEvent() throws NoSuchMethodException {
        testHandleCaseReopenedEvent(METHOD_MARK_CASE_REOPENED, EVENTS_MARK_CASE_REOPENED);
    }

    @Test
    public void shouldHandleUpdateCaseReopenedEvent() throws NoSuchMethodException {
        testHandleCaseReopenedEvent(METHOD_UPDATE_CASE_REOPENED, EVENTS_UPDATE_CASE_REOPENED);
    }

    @Test
    public void shouldHandleUndoCaseReopenedEvent() throws NoSuchMethodException {
        testHandleCaseReopenedEvent(METHOD_UNDO_CASE_REOPENED, EVENTS_UNDO_CASE_REOPENED);
    }

    @Test
    public void shouldUpdateCaseWhenMarkCaseReOpenedRequested() {
        testCaseReopenDetailsUpdated(listener::markCaseReopened, MARK_CASE_REOPEN_DETAILS);
    }

    @Test
    public void shouldUpdateCaseWhenUpdateCaseReOpenedRequested() {
        testCaseReopenDetailsUpdated(listener::updateCaseReopened, UPDATE_CASE_REOPEN_DETAILS);
    }

    @Test
    public void shouldUndoCaseWhenUndoCaseReOpenedRequested() {
        final UUID caseId = MARK_CASE_REOPEN_DETAILS.getCaseId();
        when(caseRepository.findBy(caseId)).thenReturn(caseDetails);

        final JsonEnvelope event = JsonEnvelopeBuilder.envelope()
                .with(JsonObjectMetadata.metadataWithRandomUUID("sjp.events.case-reopened-in-libra-undone"))
                .withPayloadOf(caseId, "caseId").build();
        listener.undoCaseReopened(event);

        verify(caseRepository).findBy(caseId);
        assertThat(caseDetails.getReopenedDate(), is(nullValue()));
        assertThat(caseDetails.getLibraCaseNumber(), is(nullValue()));
        assertThat(caseDetails.getReopenedInLibraReason(), is(nullValue()));
    }

    private void testCaseReopenDetailsUpdated(Consumer<JsonEnvelope> consumer, CaseReopenDetails caseReopenDetails) {


        final JsonEnvelope event = JsonEnvelopeBuilder.envelope()
                .with(JsonObjectMetadata.metadataWithRandomUUID("sjp.events.case-reopened-in-libra"))
                .withPayloadOf(caseReopenDetails.getCaseId(), "caseId")
                .withPayloadOf(caseReopenDetails.getReopenedDate().toString(), "reopenedDate")
                .withPayloadOf(caseReopenDetails.getLibraCaseNumber(), "libraCaseNumber")
                .withPayloadOf(caseReopenDetails.getReason(), "reason")
                .build();

        when(caseRepository.findBy(caseReopenDetails.getCaseId())).thenReturn(caseDetails);

        consumer.accept(event);

        verify(caseRepository).findBy(caseReopenDetails.getCaseId());
        verify(caseDetails).setReopenedDate(caseReopenDetails.getReopenedDate());
        verify(caseDetails).setLibraCaseNumber(caseReopenDetails.getLibraCaseNumber());
        verify(caseDetails).setReopenedInLibraReason(caseReopenDetails.getReason());
    }


    private void testHandleCaseReopenedEvent(String methodName, String eventName) throws NoSuchMethodException {
        Class<CaseUpdatedListener> caseUpdatedListenerClass = CaseUpdatedListener.class;

        Method m = caseUpdatedListenerClass.getDeclaredMethod(methodName, JsonEnvelope.class);
        Handles handles = m.getAnnotation(Handles.class);

        assertThat(eventName, equalTo(handles.value()));
    }
}
