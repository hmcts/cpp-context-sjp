package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionRejected;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.decision.AocpDecision;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AocpAcceptanceTimerExpiredHandlerTest {

    @Mock
    private AggregateService aggregateService;

    @Mock
    protected Stream<Object> events;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @InjectMocks
    private AocpAcceptanceTimerExpiredHandler aocpAcceptanceTimerExpiredHandler;

    @Mock
    private Enveloper enveloper;


    @Test
    public void testExpireDefendantResponseTimer() throws EventStreamException {
        final UUID caseId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final Session session = mock(Session.class);
        final AocpDecision aocpDecision = new AocpDecision(UUID.randomUUID(), sessionId, caseId, null, null);
        final Envelope<AocpDecision> envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.expire-defendant-aocp-response-timer"), aocpDecision);
        when(eventSource.getStreamById(sessionId)).thenReturn(eventStream);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(caseAggregate.expireAocpResponseTimerAndSaveDecision(any(), any())).thenReturn(events);
        when(aggregateService.get(eventStream, Session.class)).thenReturn(session);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        aocpAcceptanceTimerExpiredHandler.aocpResponseTimeExpiredAndSaveDecision(envelope);

        verify(caseAggregate).expireAocpResponseTimerAndSaveDecision(any(), any());
    }
}