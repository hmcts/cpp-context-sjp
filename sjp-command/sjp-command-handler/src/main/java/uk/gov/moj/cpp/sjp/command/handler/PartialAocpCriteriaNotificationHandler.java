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
import uk.gov.moj.cpp.sjp.domain.aggregate.PartialAocpCriteriaNotificationAggregate;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PartialAocpCriteriaNotificationHandler {

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    private static final String SENT_TIME = "sentTime";
    private static final String FAILED_TIME = "failedTime";

    @SuppressWarnings("deprecation")
    @Handles("sjp.command.update-partial-aocp-criteria-notification-to-prosecutor-status")
    public void partialAocpNotification(final JsonEnvelope envelope) throws EventStreamException {
        final UUID caseId = getCaseId(envelope);
        final ZonedDateTime sentTime = getSentTime(envelope);
        final ZonedDateTime failedTime = getFailedTime(envelope);

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final PartialAocpCriteriaNotificationAggregate aggregate = aggregateService.get(eventStream, PartialAocpCriteriaNotificationAggregate.class);

        final Stream<Object> events;

        if(sentTime != null){
            events = aggregate.markAsNotificationSent(caseId, sentTime);
        }else if(failedTime != null){
            events = aggregate.markAsNotificationFailed(caseId, failedTime);
        }else {
            events = aggregate.markAsNotificationQueued(caseId);
        }

        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    private ZonedDateTime getSentTime(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        if (jsonObject.get(SENT_TIME) != null && !jsonObject.isNull(SENT_TIME)) {
            return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString(SENT_TIME));
        }
        return null;
    }
    private ZonedDateTime getFailedTime(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();

        if (jsonObject.get(FAILED_TIME) != null && !jsonObject.isNull(FAILED_TIME)) {
            return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString(FAILED_TIME));
        }
        return null;
    }

    private UUID getCaseId(final JsonEnvelope envelope) {
        return UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"));
    }
}
