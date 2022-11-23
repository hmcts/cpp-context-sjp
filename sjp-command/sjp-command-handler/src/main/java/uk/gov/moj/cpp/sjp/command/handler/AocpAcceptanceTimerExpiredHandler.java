package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.decision.AocpDecision;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AocpAcceptanceTimerExpiredHandler extends CaseCommandHandler {

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Handles("sjp.command.expire-defendant-aocp-response-timer")
    public void aocpResponseTimeExpiredAndSaveDecision(final Envelope<AocpDecision> command) throws EventStreamException {
        final AocpDecision aocpDecision= command.payload();
        final EventStream sessionEventStream = eventSource.getStreamById(aocpDecision.getSessionId());
        final Session session = aggregateService.get(sessionEventStream, Session.class);
        applyToCaseAggregate(aocpDecision.getCaseId(), command, aggregate-> aggregate.expireAocpResponseTimerAndSaveDecision(aocpDecision, session));
    }
}
