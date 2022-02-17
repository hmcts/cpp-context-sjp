package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingInformation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;


public class CaseCommandHandler {

    static final String STREAM_ID = "caseId";

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Inject
    protected JsonObjectToObjectConverter converter;

    protected void applyToCaseAggregate(final JsonEnvelope command, final Function<CaseAggregate, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(getCaseId(command.payloadAsJsonObject()));
        final CaseAggregate aCase = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = function.apply(aCase);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    protected void applyToCaseAggregate(final UUID caseId, final Envelope<?> command, final Function<CaseAggregate, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate aCase = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = function.apply(aCase);
        final JsonEnvelope jsonEnvelope = envelopeFrom(command.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));

    }

    protected Map<UUID, Session> getSessionFromCaseAggregate(final UUID caseId) {
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate aCase = aggregateService.get(eventStream, CaseAggregate.class);
        final Set<UUID> offenceIds = aCase.getState().getOffencesWithConviction();
        final Map<UUID, Session> sessions = new HashMap<>();
        for (final UUID offenceId : offenceIds) {
            final ConvictingInformation convictionInfo = aCase.getState().getOffenceConvictionInfo(offenceId);
            final UUID sessionId = convictionInfo.getSessionId();
            final EventStream sessionEventStream = eventSource.getStreamById(sessionId);
            final Session session = aggregateService.get(sessionEventStream, Session.class);
            sessions.put(offenceId, session);
        }
        return sessions;
    }

    protected UUID getCaseId(final JsonObject payload) {
        return UUID.fromString(payload.getString(STREAM_ID));
    }

    protected UUID getUserId(final Envelope<?> command) {
        return command.metadata().userId().map(UUID::fromString).orElse(null);
    }
}