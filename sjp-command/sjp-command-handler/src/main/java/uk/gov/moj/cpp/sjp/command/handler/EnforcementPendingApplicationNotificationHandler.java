package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZonedDateTime.parse;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.EnforcementPendingApplicationRequiredNotification;
import uk.gov.moj.cpp.sjp.domain.aggregate.EnforcementPendingApplicationNotificationAggregate;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EnforcementPendingApplicationNotificationHandler extends CaseCommandHandler {
    @Inject
    private AggregateService aggregateService;
    @Inject
    private EventSource eventSource;
    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.enforcement-pending-application-check-requires-notification")
    public void checkIfNotificationRequired(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final EnforcementPendingApplicationRequiredNotification initiateNotification = new EnforcementPendingApplicationRequiredNotification(
                getCaseId(payload), getDivisionCode(payload));
        applyToCaseAggregate(command, caseAggregate -> caseAggregate.checkIfPendingApplicationToNotified(initiateNotification));
    }

    @Handles("sjp.command.enforcement-pending-application-generate-notification")
    public void generateNotification(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationId = getApplicationId(envelope);
        final UUID fileId = getFileId(envelope);
        final ZonedDateTime generatedTime = getGeneratedTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final EnforcementPendingApplicationNotificationAggregate aggregate = aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class);

        final Stream<Object> events = aggregate.markAsGenerated(applicationId, fileId, generatedTime);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("sjp.command.enforcement-pending-application-fail-generation-notification")
    public void failGenerationNotification(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationId = getApplicationId(envelope);
        final ZonedDateTime generationFailedTime = getGenerationFailedTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final EnforcementPendingApplicationNotificationAggregate aggregate = aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsGenerationFailed(applicationId, generationFailedTime);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("sjp.command.enforcement-pending-application-queue-notification")
    public void queueNotification(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationId = getApplicationId(envelope);
        final ZonedDateTime queuedTime = getQueuedTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final EnforcementPendingApplicationNotificationAggregate aggregate = aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsNotificationQueued(applicationId, queuedTime);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("sjp.command.enforcement-pending-application-send-notification")
    public void sendNotification(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationDecisionId = getApplicationId(envelope);
        final ZonedDateTime sentTime = getSentTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationDecisionId);
        final EnforcementPendingApplicationNotificationAggregate aggregate = aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsNotificationSent(applicationDecisionId, sentTime);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("sjp.command.enforcement-pending-application-fail-notification")
    public void failNotification(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationId = getApplicationId(envelope);
        final ZonedDateTime failedTime = getFailedTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final EnforcementPendingApplicationNotificationAggregate aggregate = aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class);
        final Stream<Object> events = aggregate.markAsNotificationFailed(applicationId, failedTime);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    private ZonedDateTime getGeneratedTime(final JsonEnvelope envelope) {
        return parse(envelope.payloadAsJsonObject().getString("generatedTime"));
    }

    private ZonedDateTime getGenerationFailedTime(final JsonEnvelope envelope) {
        return parse(envelope.payloadAsJsonObject().getString("generationFailedTime"));
    }

    private ZonedDateTime getQueuedTime(final JsonEnvelope envelope) {
        return parse(envelope.payloadAsJsonObject().getString("queuedTime"));
    }

    private ZonedDateTime getSentTime(final JsonEnvelope envelope) {
        return parse(envelope.payloadAsJsonObject().getString("sentTime"));
    }

    private ZonedDateTime getFailedTime(final JsonEnvelope envelope) {
        return parse(envelope.payloadAsJsonObject().getString("failedTime"));
    }

    private UUID getApplicationId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("applicationId"));
    }

    private UUID getFileId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("fileId"));
    }

    private int getDivisionCode(final JsonObject payload) {
        return payload.getInt("divisionCode");
    }

}
