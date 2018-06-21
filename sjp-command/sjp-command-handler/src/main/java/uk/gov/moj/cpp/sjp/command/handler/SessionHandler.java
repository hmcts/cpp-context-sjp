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
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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
    public void startSession(final JsonEnvelope startSessionCommand) throws EventStreamException {
        final JsonObject startSession = startSessionCommand.payloadAsJsonObject();

        final UUID userId = UUID.fromString(startSessionCommand.metadata().userId().get());
        final UUID sessionId = UUID.fromString(startSession.getString("sessionId"));
        final String courtHouseName = startSession.getString("courtHouseName");
        final String localJusticeAreaNationalCourtCode = startSession.getString("localJusticeAreaNationalCourtCode");
        final Optional<String> magistrate = Optional.ofNullable(startSession.getString("magistrate", null));

        applyToSessionAggregate(startSessionCommand, (session) -> magistrate
                .map(providedMagistrate -> session.startMagistrateSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, clock.now(), providedMagistrate))
                .orElseGet(() -> session.startDelegatedPowersSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, clock.now())));
    }

    @Handles("sjp.command.end-session")
    public void endSession(final JsonEnvelope endSessionCommand) throws EventStreamException {
        final UUID sessionId = UUID.fromString(endSessionCommand.payloadAsJsonObject().getString("sessionId"));
        applyToSessionAggregate(endSessionCommand, (session) -> session.endSession(sessionId, clock.now()));
    }

    @Handles("sjp.command.migrate-session")
    public void migrateSession(final JsonEnvelope migrateSessionCommand) throws EventStreamException {
        final JsonObject migrateSession = migrateSessionCommand.payloadAsJsonObject();

        final UUID userId = UUID.fromString(migrateSession.getString("userId"));
        final UUID sessionId = UUID.fromString(migrateSession.getString("sessionId"));
        final ZonedDateTime startedAt = ZonedDateTime.parse(migrateSession.getString("startedAt"));
        final String courtHouseName = migrateSession.getString("courtHouseName");
        final String localJusticeAreaNationalCourtCode = migrateSession.getString("localJusticeAreaNationalCourtCode");
        final Optional<String> magistrate = Optional.ofNullable(migrateSession.getString("magistrate", null));

        applyToSessionAggregate(migrateSessionCommand, (session) -> magistrate
                .map(providedMagistrate -> session.startMagistrateSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, providedMagistrate))
                .orElseGet(() -> session.startDelegatedPowersSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt)));
    }

    private void applyToSessionAggregate(JsonEnvelope sessionCommand, final Function<Session, Stream<Object>> function) throws EventStreamException {
        final UUID sessionId = UUID.fromString(sessionCommand.payloadAsJsonObject().getString("sessionId"));
        final EventStream eventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(eventStream, Session.class);

        final Stream<Object> events = function.apply(session);

        eventStream.append(events.map(enveloper.withMetadataFrom(sessionCommand)));
    }


}
