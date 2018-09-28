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

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AcknowledgeDefendantDetailsUpdatesHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Clock clock;

    @Handles("sjp.command.acknowledge-defendant-details-updates")
    public void acknowledgeDefendantDetailsUpdates(
            final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = caseAggregate
                .acknowledgeDefendantDetailsUpdates(defendantId, clock.now());

        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

}
