package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCasesRepository;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private ReadyCasesRepository readyCasesRepository;

    @InjectMocks
    private ReadyCaseListener readyCaseListener;

    @Captor
    private ArgumentCaptor<ReadyCase> readyCasesCaptor;

    private final CaseReadinessReason reason = CaseReadinessReason.PLEADED_GUILTY;
    private final UUID caseId = randomUUID();

    @Test
    public void shouldHandleCaseMarkedReadyForDecision() {

        final JsonEnvelope caseMarkedReadyForDecisionEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-marked-ready-for-decision"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("reason", reason.name())
                        .add("markedAt", LocalDateTime.now(UTC).toString())
                        .build());

        readyCaseListener.handleCaseMarkedReadyForDecisison(caseMarkedReadyForDecisionEvent);

        verify(readyCasesRepository).save(readyCasesCaptor.capture());

        final ReadyCase readyCase = readyCasesCaptor.getValue();

        assertThat(readyCase.getReason(), equalTo(reason));
        assertThat(readyCase.getCaseId(), equalTo(caseId));
    }

    @Test
    public void shouldHandleCaseUnmarkedReadyForDecision() {

        final JsonEnvelope caseUnmarkedReadyForDecisionEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-unmarked-ready-for-decision"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        readyCaseListener.handleCaseUnmarkedReadyForDecision(caseUnmarkedReadyForDecisionEvent);

        verify(readyCasesRepository).remove(readyCasesCaptor.capture());
    }

}
