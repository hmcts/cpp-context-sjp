package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import java.time.temporal.ChronoUnit;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.PartialAocpCriteriaNotificationAggregate;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorFailed;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorQueued;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorSent;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class PartialAocpCriteriaNotificationHandlerTest {

    public static final String PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND = "sjp.command.update-partial-aocp-criteria-notification-to-prosecutor-status";

    @InjectMocks
    private PartialAocpCriteriaNotificationHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private PartialAocpCriteriaNotificationAggregate aggregate;

    private final String CASE_ID_KEY_STRING = "caseId";
    private final UUID CASE_ID_UUID = UUID.randomUUID();
    private final String CASE_ID_STR = CASE_ID_UUID.toString();

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            PartialAocpCriteriaNotificationProsecutorQueued.class,
            PartialAocpCriteriaNotificationProsecutorSent.class,
            PartialAocpCriteriaNotificationProsecutorFailed.class);



    @Test
    public void shouldHandleQueueNotificationCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("partialAocpNotification")
                        .thatHandles(PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND)
                ));
    }


    @Test
    public void shouldRequestEmailQueuedNotification() throws EventStreamException {
        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(CASE_ID_KEY_STRING, CASE_ID_STR)
        );

        final PartialAocpCriteriaNotificationProsecutorQueued event = new PartialAocpCriteriaNotificationProsecutorQueued(CASE_ID_UUID);

        when(eventSource.getStreamById(CASE_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, PartialAocpCriteriaNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationQueued(CASE_ID_UUID)).thenReturn(Stream.of(event));

        handler.partialAocpNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(PartialAocpCriteriaNotificationProsecutorQueued.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID_STR))
                                ))))));
    }

    @Test
    public void shouldRequestEmailSendNotification() throws EventStreamException {
        final ZonedDateTime sentAt = ZonedDateTime.now(ZoneOffset.UTC);

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(CASE_ID_KEY_STRING, CASE_ID_STR)
                        .add("sentTime", sentAt.toString())
        );


        final PartialAocpCriteriaNotificationProsecutorSent event = new PartialAocpCriteriaNotificationProsecutorSent(CASE_ID_UUID, sentAt);

        when(eventSource.getStreamById(CASE_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, PartialAocpCriteriaNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationSent(CASE_ID_UUID, sentAt)).thenReturn(Stream.of(event));

        handler.partialAocpNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(PartialAocpCriteriaNotificationProsecutorSent.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID_STR)),
                                        withJsonPath("$.sentTime", equalTo(sentAt.toLocalDateTime().truncatedTo(ChronoUnit.MILLIS) + "Z"))
                                ))))));
    }

    @Test
    public void shouldRequestEmailFailedNotification() throws EventStreamException {
        final ZonedDateTime failedTime = ZonedDateTime.now(ZoneOffset.UTC);

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(CASE_ID_KEY_STRING, CASE_ID_STR)
                        .add("failedTime", failedTime.toString())
        );


        final PartialAocpCriteriaNotificationProsecutorFailed event = new PartialAocpCriteriaNotificationProsecutorFailed(CASE_ID_UUID, failedTime);

        when(eventSource.getStreamById(CASE_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, PartialAocpCriteriaNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationFailed(CASE_ID_UUID, failedTime)).thenReturn(Stream.of(event));

        handler.partialAocpNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(PartialAocpCriteriaNotificationProsecutorFailed.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID_STR)),
                                        withJsonPath("$.failedTime", equalTo(failedTime.toLocalDateTime().truncatedTo(ChronoUnit.MILLIS) + "Z"))
                                ))))));
    }
}
