package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CasePublishStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReadyCaseListenerTest {

    private static final LocalDate POSTING_DATE = now().minusDays(15);
    private static final LocalDate MARKED_AT_DATE = now();
    private static final String PROSECUTOR_TFL = "TFL";
    private final UUID caseId = randomUUID();
    @Mock
    private ReadyCaseRepository readyCaseRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CasePublishStatusRepository casePublishStatusRepository;
    private CaseDetail caseDetail;
    @InjectMocks
    private ReadyCaseListener readyCaseListener;
    @Captor
    private ArgumentCaptor<ReadyCase> readyCasesCaptor;
    @Captor
    private ArgumentCaptor<CasePublishStatus> casePublishedStatusCaptor;

    @Test
    public void shouldHandleCaseMarkedReadyForDecision() {
        setupAndMarkReadyForDecision(null);
        verify(readyCaseRepository).save(readyCasesCaptor.capture());
        verify(casePublishStatusRepository).save(casePublishedStatusCaptor.capture());
        checkReadyCase();
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
    public void shouldKeepTheAssigneeSameIfTheCaseIsMarkedForReadyAgain() {
        setupAndMarkReadyForDecision(null);
        final UUID assigneeId = UUID.randomUUID();
        final ReadyCase readyCase = new ReadyCase(caseId, PIA, assigneeId, MAGISTRATE, 1, "TFL", POSTING_DATE, MARKED_AT_DATE);

        assertThat(readyCase.getReason(), equalTo(PIA));
        assertThat(readyCase.getCaseId(), equalTo(caseId));
        assertThat(readyCase.getAssigneeId().get(), equalTo(assigneeId));
        assertThat(readyCase.getPostingDate(), equalTo(POSTING_DATE));
        assertThat(readyCase.getPriority(), equalTo(1));
        assertThat(readyCase.getProsecutionAuthority(), equalTo(PROSECUTOR_TFL));
        assertThat(readyCase.getSessionType(), equalTo(MAGISTRATE));
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
        caseDetail = new CaseDetail();
        caseDetail.setProsecutingAuthority(PROSECUTOR_TFL);
        caseDetail.setPostingDate(POSTING_DATE);
        final JsonEnvelope caseMarkedReadyForDecisionEvent = givenCaseMarkedReadyForDecisionEventIsRaised();
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(casePublishStatusRepository.findBy(caseId)).thenReturn(casePublishStatus);
        whenCaseMarkedReadyForDecisionEventIsProcessed(caseMarkedReadyForDecisionEvent);
    }

    private void whenCaseMarkedReadyForDecisionEventIsProcessed(final JsonEnvelope caseMarkedReadyForDecisionEvent) {
        readyCaseListener.handleCaseMarkedReadyForDecision(caseMarkedReadyForDecisionEvent);
    }

    private JsonEnvelope givenCaseMarkedReadyForDecisionEventIsRaised() {
        return envelopeFrom(metadataWithRandomUUID("sjp.events.case-marked-ready-for-decision"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("reason", CaseReadinessReason.PLEADED_GUILTY.name())
                        .add("markedAt", OffsetDateTime.now(ZoneOffset.UTC).toString())
                        .add("sessionType", MAGISTRATE.name())
                        .add("priority", Priority.MEDIUM.name())
                        .build());
    }

    private JsonEnvelope givenCaseUnMarkedEventIsRaised() {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString());
        Optional.of(PleaType.GUILTY).ifPresent(plea -> payload.add("pleaType", plea.name()));
        return envelopeFrom(metadataWithRandomUUID("sjp.events.case-unmarked-ready-for-decision"),
                payload.build());
    }

    private void whenCaseUnMarkForDecisionEventIsProcessed(final JsonEnvelope caseUnmarkedReadyForDecisionEvent) {
        readyCaseListener.handleCaseUnmarkedReadyForDecision(caseUnmarkedReadyForDecisionEvent);
    }

    private void checkReadyCase() {
        final ReadyCase readyCase = readyCasesCaptor.getValue();
        assertThat(readyCase.getReason(), equalTo(PLEADED_GUILTY));
        assertThat(readyCase.getCaseId(), equalTo(caseId));
        assertThat(readyCase.getAssigneeId().isPresent(), equalTo(false));
        assertThat(readyCase.getPostingDate(), equalTo(POSTING_DATE));
        assertThat(readyCase.getPriority(), equalTo(2));
        assertThat(readyCase.getProsecutionAuthority(), equalTo(PROSECUTOR_TFL));
        assertThat(readyCase.getSessionType(), equalTo(MAGISTRATE));
    }

}
