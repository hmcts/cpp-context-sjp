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

    @InjectMocks
    private ReadyCaseListener readyCaseListener;

    @Captor
    private ArgumentCaptor<ReadyCase> readyCasesCaptor;

    private final UUID caseId = randomUUID();

    @Test
    public void shouldHandleCaseMarkedReadyForDecision() {

        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised(PLEADED_GUILTY);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);

        verify(readyCaseRepository).save(readyCasesCaptor.capture());
        final ReadyCase readyCase = readyCasesCaptor.getValue();

        assertThat(readyCase.getReason(), equalTo(PLEADED_GUILTY));
        assertThat(readyCase.getCaseId(), equalTo(caseId));
    }

    @Test
    public void shouldHandleCaseUnmarkedReadyForDecision() {
        final JsonEnvelope caseUnmarkedReadyForDecisionEvent = givenCaseUnMarkedEventIsRaised(PleaType.GUILTY);
        when(readyCaseRepository.findBy(UUID.fromString(caseId.toString()))).thenReturn(new ReadyCase());

        whenCaseUnMarkForDecisionEventIsProcessed(caseUnmarkedReadyForDecisionEvent);

        verify(readyCaseRepository).remove(readyCasesCaptor.capture());
    }

    private void whenCaseMarkedReadyForDecisionEventIsProcessed(JsonEnvelope caseMarkedReadyForDecisionEvent) {
        readyCaseListener.handleCaseMarkedReadyForDecision(caseMarkedReadyForDecisionEvent);
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
