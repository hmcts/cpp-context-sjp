package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReadyCaseListenerTest {

    @Mock
    private ReadyCaseRepository readyCaseRepository;

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private ReadyCaseListener readyCaseListener;

    @Captor
    private ArgumentCaptor<ReadyCase> readyCasesCaptor;

    private final UUID caseId = randomUUID();

    @Test
    public void shouldHandleCaseMarkedReadyForDecision() {

        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PLEADED_GUILTY);
        caseDetailsIsQueriedWithCaseStatus(NO_PLEA_RECEIVED);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);

        verify(readyCaseRepository).save(readyCasesCaptor.capture());
        final ReadyCase readyCase = readyCasesCaptor.getValue();

        assertThat(readyCase.getReason(), equalTo(PLEADED_GUILTY));
        assertThat(readyCase.getCaseId(), equalTo(caseId));
    }

    @Test
    public void shouldHandleCaseUnmarkedReadyForDecision() {

        caseDetailsIsQueriedWithCaseStatus(NO_PLEA_RECEIVED);

        final JsonEnvelope caseUnmarkedReadyForDecisionEvent = givenCaseUnMarkedEventIsRaised(PleaType.GUILTY);
        when(readyCaseRepository.findBy(UUID.fromString(caseId.toString()))).thenReturn(new ReadyCase());

        whenCaseUnMarkForDecisionEventIsProcessed(caseUnmarkedReadyForDecisionEvent);

        verify(readyCaseRepository).remove(readyCasesCaptor.capture());
    }

    @Test
    //When no plea received and certificate of service date >= 28 days
    public void caseStatusShouldNoPleaReceivedReadyForDecisionWhenPIA(){
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PIA);
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(NO_PLEA_RECEIVED);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, NO_PLEA_RECEIVED_READY_FOR_DECISION);
    }

    /*
        NOTE: When a withdrawal is cancelled the case status goes back to the relevant status based on the rules above i.e.
        'No plea received' when no plea received and the certificate of service date < 28 days,
        'Plea received - ready for decision'  when there is a Guilty plea etc.
    */
    @Test
    public void caseStatusShouldNoPleaReceivedReadyForDecisionWhenWithdrawalRequestCancelled(){
        //activiti raise case mark ready with PIA, because withdrawal request is cancelled
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PIA);
        //case is in withdrawal request
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(WITHDRAWAL_REQUEST_READY_FOR_DECISION);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, NO_PLEA_RECEIVED_READY_FOR_DECISION);
    }

    /*
    NOTE: When a withdrawal is cancelled the case status goes back to the relevant status based on the rules above i.e.
    'No plea received' when no plea received and the certificate of service date < 28 days,
    'Plea received - ready for decision'  when there is a Guilty plea etc.
*/
    @Test
    public void caseStatusShouldPleaReceivedReadyForDecisionWhenWithdrawalRequestCancelled(){
        //activiti raise case mark ready with PLEADED_GUILTY, because withdrawal request is cancelled
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PLEADED_GUILTY);
        //case is in withdrawal request
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(WITHDRAWAL_REQUEST_READY_FOR_DECISION);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
    }

    @Test
    public void caseStatusShouldPleaReceivedReadyForDecisionForPleaNotGuiltyAndDatesToAvoidSet(){
        //activiti raise case mark ready with PLEADED_NOT_GUILTY, because withdrawal request is cancelled
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PLEADED_NOT_GUILTY);
        //case is in withdrawal request
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(WITHDRAWAL_REQUEST_READY_FOR_DECISION);
        caseDetail.setDatesToAvoid("2018-12-12");
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
    }

    @Test
    public void caseStatusShouldPleaReceivedReadyForDecisionForPleaNotGuiltyAndPleaUpdatedDateIsMoreThan10Days(){
        //activiti raise case mark ready with PLEADED_NOT_GUILTY, because plea updated date is more than 10 days
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PLEADED_NOT_GUILTY);
        //case is in PLEA_RECEIVED_NOT_READY_FOR_DECISION
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
    }

    /*
    NOTE: When a withdrawal is cancelled the case status goes back to the relevant status based on the rules above i.e.
    'No plea received' when no plea received and the certificate of service date < 28 days,
    'Plea received - ready for decision'  when there is a Guilty plea etc.
    */
    @Test
    public void caseStatusShouldPleaReceivedReadyForDecisionWhenWithdrawalRequestCancelledForGuiltyRequestHearing(){
        //activiti raise case mark ready with PLEADED_GUILTY_REQUEST_HEARING, because withdrawal request is cancelled
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PLEADED_GUILTY_REQUEST_HEARING);
        //case is in withdrawal request
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(WITHDRAWAL_REQUEST_READY_FOR_DECISION);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
    }

    /*
    NOTE: When a withdrawal is cancelled the case status goes back to the relevant status based on the rules above
    i.e. 'No plea received' when no plea received and the certificate of service date < 28 days,
     'Plea received - ready for decision'  when there is a Guilty plea etc.
    */
    @Test
    public void caseStatusShouldNoPleaReceivedWhenWithdrawalRequestCancelled(){
        //activiti raise case unmark ready with null plea, because withdrawal request is cancelled
        final JsonEnvelope caseUnMarkedReadyForDecisionEvent = givenCaseUnMarkedEventIsRaised(null);
        //case is in withdrawal request
        final ReadyCase readyCase = new ReadyCase(caseId, CaseReadinessReason.PLEADED_NOT_GUILTY);
        when(readyCaseRepository.findBy(UUID.fromString(caseId.toString()))).thenReturn(readyCase);
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(WITHDRAWAL_REQUEST_READY_FOR_DECISION);
        whenCaseUnMarkForDecisionEventIsProcessed(caseUnMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.NO_PLEA_RECEIVED);
    }

    /*
    NOTE: When a withdrawal is cancelled the case status goes back to the relevant status based on the rules above
    i.e. 'No plea received' when no plea received and the certificate of service date < 28 days,
     'Plea received - ready for decision'  when there is a Guilty plea etc.
    */
    @Test
    public void caseStatusShouldPleaReceivedNotReadyForDecisionWhenWithdrawalRequestCancelled(){
        //activiti raise case unmark with NOT_GUILTY, because withdrawal request is cancelled
        final JsonEnvelope caseUnMarkedReadyForDecisionEvent = givenCaseUnMarkedEventIsRaised(PleaType.NOT_GUILTY);
        //case is in withdrawal request
        final ReadyCase readyCase = new ReadyCase(caseId, CaseReadinessReason.PLEADED_NOT_GUILTY);
        when(readyCaseRepository.findBy(UUID.fromString(caseId.toString()))).thenReturn(readyCase);
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(WITHDRAWAL_REQUEST_READY_FOR_DECISION);
        whenCaseUnMarkForDecisionEventIsProcessed(caseUnMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION);
    }

    @Test
    public void caseStatusShouldPleaReceivedNotReadyForDecisionWhenPleadedNotGuilty(){
        //activiti raise case unmark with NOT_GUILTY
        final JsonEnvelope caseUnMarkedReadyForDecisionEvent = givenCaseUnMarkedEventIsRaised(PleaType.NOT_GUILTY);
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(CaseStatus.NO_PLEA_RECEIVED);
        final ReadyCase readyCase = new ReadyCase(caseId, CaseReadinessReason.PLEADED_NOT_GUILTY);
        when(readyCaseRepository.findBy(UUID.fromString(caseId.toString()))).thenReturn(readyCase);
        whenCaseUnMarkForDecisionEventIsProcessed(caseUnMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION);
    }

    @Test
    public void caseStatusShouldPleaReceivedReadyForDecisionWhenPleadedNotGuiltyAndDatesToAvoidSet(){
        //activiti raise case unmark with NOT_GUILTY
        final JsonEnvelope caseUnMarkedReadyForDecisionEvent = givenCaseUnMarkedEventIsRaised(PleaType.NOT_GUILTY);
        final CaseDetail caseDetail = caseDetailsIsQueriedWithCaseStatus(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION);
        caseDetail.setDatesToAvoid("2018-12-12");
        final ReadyCase readyCase = new ReadyCase(caseId, CaseReadinessReason.PLEADED_NOT_GUILTY);
        when(readyCaseRepository.findBy(UUID.fromString(caseId.toString()))).thenReturn(readyCase);
        whenCaseUnMarkForDecisionEventIsProcessed(caseUnMarkedReadyForDecisionEvent);
        thenCaseStatusIsSetTo(caseDetail, CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
    }

    private void thenCaseStatusIsSetTo(CaseDetail caseDetail, CaseStatus caseStatus) {
        assertThat(caseDetail.getStatus(), is(caseStatus));
    }

    private void whenCaseMarkedReadyForDecisionEventIsProcessed(JsonEnvelope caseMarkedReadyForDecisionEvent) {
        readyCaseListener.handleCaseMarkedReadyForDecision(caseMarkedReadyForDecisionEvent);
    }

    private CaseDetail caseDetailsIsQueriedWithCaseStatus(CaseStatus caseStatus) {
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setStatus(caseStatus);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        return caseDetail;
    }

    private JsonEnvelope givenCaseMarkedReadyForDecisionEventIsRaised(CaseReadinessReason caseReadinessReason) {
        return envelopeFrom(metadataWithRandomUUID("sjp.events.case-marked-ready-for-decision"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("reason", caseReadinessReason.name())
                        .add("markedAt", LocalDateTime.now(UTC).toString())
                        .build());
    }

    private JsonEnvelope givenCaseUnMarkedEventIsRaised(PleaType pleaType) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString());
        ofNullable(pleaType).ifPresent(plea -> payload.add("pleaType", plea.name()));
        return envelopeFrom(metadataWithRandomUUID("sjp.events.case-unmarked-ready-for-decision"),
                payload.build());
    }

        private void whenCaseUnMarkForDecisionEventIsProcessed(JsonEnvelope caseUnmarkedReadyForDecisionEvent) {
        readyCaseListener.handleCaseUnmarkedReadyForDecision(caseUnmarkedReadyForDecisionEvent);
    }

}
