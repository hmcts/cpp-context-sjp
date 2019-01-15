package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CasePublishStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.time.LocalDateTime;
import java.util.Optional;
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

    @Mock
    private CasePublishStatusRepository casePublishStatusRepository;

    @Mock
    private CaseDetail caseDetail;

    @InjectMocks
    private ReadyCaseListener readyCaseListener;

    @Captor
    private ArgumentCaptor<ReadyCase> readyCasesCaptor;

    @Captor
    private ArgumentCaptor<CasePublishStatus> casePublishedStatusCaptor;

    private final UUID caseId = randomUUID();

    @Test
    public void shouldHandleCaseMarkedReadyForDecision() {
        setupAndMarkReadyForDecision(null);
        verify(readyCaseRepository).save(readyCasesCaptor.capture());
        verify(casePublishStatusRepository).save(casePublishedStatusCaptor.capture());

        final ReadyCase readyCase = readyCasesCaptor.getValue();
        assertThat(readyCase.getReason(), equalTo(PLEADED_GUILTY));
        assertThat(readyCase.getCaseId(), equalTo(caseId));
    }

    @Test
    public void shouldHandleCaseMarkedReadyForDecisionForExistingCasePublishedStatus() {
        setupAndMarkReadyForDecision(new CasePublishStatus());

        verify(readyCaseRepository).save(readyCasesCaptor.capture());
        verify(casePublishStatusRepository, never()).save(casePublishedStatusCaptor.capture());

        final ReadyCase readyCase = readyCasesCaptor.getValue();
        assertThat(readyCase.getReason(), equalTo(PLEADED_GUILTY));
        assertThat(readyCase.getCaseId(), equalTo(caseId));
    }

    @Test
    public void shouldHandleCaseUnmarkedReadyForDecision() {
        final JsonEnvelope caseUnmarkedReadyForDecisionEvent = givenCaseUnMarkedEventIsRaised();
        when(readyCaseRepository.findBy(caseId)).thenReturn(new ReadyCase());
        when(casePublishStatusRepository.findBy(caseId)).thenReturn(new CasePublishStatus());

        whenCaseUnMarkForDecisionEventIsProcessed(caseUnmarkedReadyForDecisionEvent);

        verify(readyCaseRepository).remove(readyCasesCaptor.capture());
        verify(casePublishStatusRepository).save(casePublishedStatusCaptor.capture());
    }

    private void setupAndMarkReadyForDecision(final CasePublishStatus casePublishStatus) {
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised();
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(casePublishStatusRepository.findBy(caseId)).thenReturn(casePublishStatus);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
    }

    private void whenCaseMarkedReadyForDecisionEventIsProcessed(JsonEnvelope caseMarkedReadyForDecisionEvent) {
        readyCaseListener.handleCaseMarkedReadyForDecision(caseMarkedReadyForDecisionEvent);
    }

    private JsonEnvelope givenCaseMarkedReadyForDecisionEventIsRaised() {
        return envelopeFrom(metadataWithRandomUUID("sjp.events.case-marked-ready-for-decision"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("reason", CaseReadinessReason.PLEADED_GUILTY.name())
                        .add("markedAt", LocalDateTime.now(UTC).toString())
                        .build());
    }

    private JsonEnvelope givenCaseUnMarkedEventIsRaised() {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString());
        Optional.of(PleaType.GUILTY).ifPresent(plea -> payload.add("pleaType", plea.name()));
        return envelopeFrom(metadataWithRandomUUID("sjp.events.case-unmarked-ready-for-decision"),
                payload.build());
    }

    private void whenCaseUnMarkForDecisionEventIsProcessed(JsonEnvelope caseUnmarkedReadyForDecisionEvent) {
        readyCaseListener.handleCaseUnmarkedReadyForDecision(caseUnmarkedReadyForDecisionEvent);
    }

}
