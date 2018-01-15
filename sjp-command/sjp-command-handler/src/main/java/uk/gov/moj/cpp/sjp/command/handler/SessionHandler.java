package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class SessionHandler {

    @Inject
    private Clock clock;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.start-session")
    public void startSession(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final UUID legalAdviserId = UUID.fromString(command.metadata().userId().get());
        final UUID sessionId = UUID.fromString(payload.getString("sessionId"));
        final String magistrate = payload.getString("magistrate", null);
        final String courtCode = payload.getString("courtCode");

        final EventStream eventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(eventStream, Session.class);
        final Stream<Object> startSessionEvents = session.startSession(sessionId, legalAdviserId, courtCode, magistrate, clock.now());
        eventStream.append(startSessionEvents.map(enveloper.withMetadataFrom(command)));
    }

}
