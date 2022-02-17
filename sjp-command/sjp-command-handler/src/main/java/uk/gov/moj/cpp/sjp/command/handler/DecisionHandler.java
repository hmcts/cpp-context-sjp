package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.json.schemas.domains.sjp.commands.SaveApplicationDecision;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class DecisionHandler extends CaseCommandHandler {

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Handles("sjp.command.save-decision")
    public void saveDecision(final Envelope<Decision> command) throws EventStreamException {
        final UUID sessionId = command.payload().getSessionId();
        final EventStream sessionEventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(sessionEventStream, Session.class);
        applyToCaseAggregate(command.payload().getCaseId(), command, aCase -> aCase.saveDecision(command.payload(), session));
    }

    @Handles("sjp.command.handler.save-application-decision")
    public void saveApplicationDecision(final Envelope<SaveApplicationDecision> command) throws EventStreamException {
        final UUID sessionId = command.payload().getSessionId();
        final EventStream sessionEventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(sessionEventStream, Session.class);
        applyToCaseAggregate(command.payload().getCaseId(), command, aCase -> aCase.saveApplicationDecision(command.payload(), session));
    }

}
