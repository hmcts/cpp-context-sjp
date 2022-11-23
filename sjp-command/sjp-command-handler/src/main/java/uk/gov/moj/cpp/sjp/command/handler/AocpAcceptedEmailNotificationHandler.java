package uk.gov.moj.cpp.sjp.command.handler;

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
import uk.gov.moj.cpp.sjp.domain.aggregate.AocpAcceptedEmailNotificationAggregate;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AocpAcceptedEmailNotificationHandler {

    @Inject
    private AggregateService aggregateService;
    @Inject
    private EventSource eventSource;
    @Inject
    private Enveloper enveloper;

    private static final String FAILED_TIME = "failedTime";
    private static final String QUEUED_TIME = "queuedTime";
    private static final String SENT_TIME = "sentTime";

    @Handles("sjp.command.update-aocp-acceptance-email-notification")
    public void notification(final JsonEnvelope envelope) throws EventStreamException {
        final UUID applicationId = getCaseId(envelope);
        final ZonedDateTime queuedTime = getQueuedTime(envelope);
        final ZonedDateTime sentTime = getSentTime(envelope);
        final ZonedDateTime failedTime = getFailedTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final AocpAcceptedEmailNotificationAggregate aggregate = aggregateService.get(eventStream, AocpAcceptedEmailNotificationAggregate.class);

        final Stream<Object> events;

        if(sentTime != null){
            events = aggregate.markAsNotificationSent(applicationId, sentTime);
        }else if(failedTime != null){
            events = aggregate.markAsNotificationFailed(applicationId, failedTime);
        }else {
            events = aggregate.markAsNotificationQueued(applicationId, queuedTime);
        }

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    private UUID getCaseId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("caseId"));
    }

    private ZonedDateTime getFailedTime(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();

        if (jsonObject.get(FAILED_TIME) != null && !jsonObject.isNull(FAILED_TIME)) {
            return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString(FAILED_TIME));
        }
        return null;
    }

    private ZonedDateTime getQueuedTime(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();

        if (jsonObject.get(QUEUED_TIME) != null && !jsonObject.isNull(QUEUED_TIME)) {
            return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString(QUEUED_TIME));
        }
        return null;
    }

    private ZonedDateTime getSentTime(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        if (jsonObject.get(SENT_TIME) != null && !jsonObject.isNull(SENT_TIME)) {
            return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString(SENT_TIME));
        }
        return null;
    }
}
