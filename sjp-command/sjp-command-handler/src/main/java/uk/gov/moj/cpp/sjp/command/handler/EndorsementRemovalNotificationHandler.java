package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.EndorsementRemovalNotificationAggregate;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EndorsementRemovalNotificationHandler {

    @Inject
    private AggregateService aggregateService;
    @Inject
    private EventSource eventSource;
    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.endorsement-removal-notification-generated")
    public void generated(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        final UUID fileId = getFileId(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationDecisionId);
        final EndorsementRemovalNotificationAggregate aggregate = aggregateService.get(eventStream, EndorsementRemovalNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsGenerated(applicationDecisionId, fileId);

        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("sjp.command.endorsement-removal-notification-generation-failed")
    public void generationFailed(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationDecisionId);
        final EndorsementRemovalNotificationAggregate aggregate = aggregateService.get(eventStream, EndorsementRemovalNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsGenerationFailed(applicationDecisionId);

        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("sjp.command.endorsement-removal-notification-queued")
    public void notificationQueued(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationDecisionId);
        final EndorsementRemovalNotificationAggregate aggregate = aggregateService.get(eventStream, EndorsementRemovalNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsNotificationQueued(applicationDecisionId);

        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("sjp.command.endorsement-removal-notification-sent")
    public void notificationSent(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        final ZonedDateTime sentTime = getSentTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationDecisionId);
        final EndorsementRemovalNotificationAggregate aggregate = aggregateService.get(eventStream, EndorsementRemovalNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsNotificationSent(applicationDecisionId, sentTime);

        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("sjp.command.endorsement-removal-notification-failed")
    public void notificationFailed(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        final ZonedDateTime failedTime = getFailedTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationDecisionId);
        final EndorsementRemovalNotificationAggregate aggregate = aggregateService.get(eventStream, EndorsementRemovalNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsNotificationFailed(applicationDecisionId, failedTime);

        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    private ZonedDateTime getSentTime(final JsonEnvelope envelope) {
        return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString("sentTime"));
    }
    private ZonedDateTime getFailedTime(final JsonEnvelope envelope) {
        return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString("failedTime"));
    }

    private UUID getApplicationDecisionId(final JsonEnvelope envelope) {
        return UUID.fromString(envelope.payloadAsJsonObject().getString("applicationDecisionId"));
    }

    private UUID getFileId(final JsonEnvelope envelope) {
        return UUID.fromString(envelope.payloadAsJsonObject().getString("fileId"));
    }
}
