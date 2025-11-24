package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.temporal.ChronoUnit.MILLIS;
import static javax.json.Json.createObjectBuilder;
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

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.EnforcementPendingApplicationNotificationAggregate;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerated;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationQueued;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationSent;

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
public class EnforcementPendingApplicationNotificationHandlerTest {

    public static final String ENFORCEMENT_PENDING_APPLICATION_GENERATE_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-generate-notification";
    public static final String ENFORCEMENT_PENDING_APPLICATION_FAIL_GENERATION_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-fail-generation-notification";
    public static final String ENFORCEMENT_PENDING_APPLICATION_QUEUE_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-queue-notification";
    public static final String ENFORCEMENT_PENDING_APPLICATION_SEND_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-send-notification";
    public static final String ENFORCEMENT_PENDING_APPLICATION_FAIL_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-fail-notification";

    @InjectMocks
    private EnforcementPendingApplicationNotificationHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private EnforcementPendingApplicationNotificationAggregate aggregate;

    private final String APPLICATION_ID_KEY_STRING = "applicationId";
    private final UUID APPLICATION_ID_UUID = UUID.randomUUID();
    private final String APPLICATION_ID_STR = APPLICATION_ID_UUID.toString();

    private final String FILE_ID_KEY_STRING = "fileId";
    private final UUID FILE_ID_UUID = UUID.randomUUID();
    private final String FILE_ID_STR = FILE_ID_UUID.toString();

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            EnforcementPendingApplicationNotificationGenerated.class,
            EnforcementPendingApplicationNotificationGenerationFailed.class,
            EnforcementPendingApplicationNotificationQueued.class,
            EnforcementPendingApplicationNotificationSent.class,
            EnforcementPendingApplicationNotificationFailed.class);

    @Test
    public void shouldHandleGenerateNotificationCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("generateNotification")
                        .thatHandles(ENFORCEMENT_PENDING_APPLICATION_GENERATE_NOTIFICATION_COMMAND)
                ));
    }

    @Test
    public void shouldHandleGenerateNotificationFailedCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("failGenerationNotification")
                        .thatHandles(ENFORCEMENT_PENDING_APPLICATION_FAIL_GENERATION_NOTIFICATION_COMMAND)
                ));
    }

    @Test
    public void shouldHandleQueueNotificationCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("queueNotification")
                        .thatHandles(ENFORCEMENT_PENDING_APPLICATION_QUEUE_NOTIFICATION_COMMAND)
                ));
    }

    @Test
    public void shouldHandleSentNotificationCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("sendNotification")
                        .thatHandles(ENFORCEMENT_PENDING_APPLICATION_SEND_NOTIFICATION_COMMAND)
                ));
    }


    @Test
    public void shouldHandleFailNotificationCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("failNotification")
                        .thatHandles(ENFORCEMENT_PENDING_APPLICATION_FAIL_NOTIFICATION_COMMAND)
                ));
    }

    @Test
    public void shouldRequestEmailAttachmentGenerationNotification() throws EventStreamException {
        final ZonedDateTime generationTime = new UtcClock().now();

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(ENFORCEMENT_PENDING_APPLICATION_GENERATE_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(APPLICATION_ID_KEY_STRING, APPLICATION_ID_STR)
                        .add(FILE_ID_KEY_STRING, FILE_ID_STR)
                        .add("generatedTime", generationTime.toString())
        );

        final EnforcementPendingApplicationNotificationGenerated event = new EnforcementPendingApplicationNotificationGenerated(APPLICATION_ID_UUID, FILE_ID_UUID, generationTime);

        when(eventSource.getStreamById(APPLICATION_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsGenerated(APPLICATION_ID_UUID, FILE_ID_UUID, generationTime)).thenReturn(Stream.of(event));

        handler.generateNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(EnforcementPendingApplicationNotificationGenerated.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", equalTo(APPLICATION_ID_STR)),
                                        withJsonPath("$.fileId", equalTo(FILE_ID_STR)),
                                        withJsonPath("$.generatedTime", equalTo(generationTime.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldRequestEmailAttachmentGenerationFailedNotification() throws EventStreamException {
        final ZonedDateTime generationFailedTime = new UtcClock().now();

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(ENFORCEMENT_PENDING_APPLICATION_FAIL_GENERATION_NOTIFICATION_COMMAND),
                createObjectBuilder().add(APPLICATION_ID_KEY_STRING, APPLICATION_ID_STR)
                        .add("generationFailedTime", generationFailedTime.toString())
        );

        final EnforcementPendingApplicationNotificationGenerationFailed event = new EnforcementPendingApplicationNotificationGenerationFailed(APPLICATION_ID_UUID, generationFailedTime);

        when(eventSource.getStreamById(APPLICATION_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsGenerationFailed(APPLICATION_ID_UUID, generationFailedTime)).thenReturn(Stream.of(event));

        handler.failGenerationNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(EnforcementPendingApplicationNotificationGenerationFailed.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", equalTo(APPLICATION_ID_STR)),
                                        withJsonPath("$.generationFailedTime", equalTo(generationFailedTime.toLocalDateTime() + "Z"))
                                ))))));
    }


    @Test
    public void shouldRequestEmailQueuedNotification() throws EventStreamException {
        final ZonedDateTime queuedTime = new UtcClock().now();
        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(ENFORCEMENT_PENDING_APPLICATION_QUEUE_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(APPLICATION_ID_KEY_STRING, APPLICATION_ID_STR)
                        .add("queuedTime", queuedTime.toString())
        );

        final EnforcementPendingApplicationNotificationQueued event = new EnforcementPendingApplicationNotificationQueued(APPLICATION_ID_UUID, queuedTime);

        when(eventSource.getStreamById(APPLICATION_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationQueued(APPLICATION_ID_UUID, queuedTime)).thenReturn(Stream.of(event));

        handler.queueNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(EnforcementPendingApplicationNotificationQueued.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", equalTo(APPLICATION_ID_STR)),
                                        withJsonPath("$.queuedTime", equalTo(queuedTime.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldRequestEmailSendNotification() throws EventStreamException {
        final ZonedDateTime sentAt = new UtcClock().now();

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(ENFORCEMENT_PENDING_APPLICATION_SEND_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(APPLICATION_ID_KEY_STRING, APPLICATION_ID_STR)
                        .add("sentTime", sentAt.toString())
        );


        final EnforcementPendingApplicationNotificationSent event = new EnforcementPendingApplicationNotificationSent(APPLICATION_ID_UUID, sentAt);

        when(eventSource.getStreamById(APPLICATION_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationSent(APPLICATION_ID_UUID, sentAt)).thenReturn(Stream.of(event));

        handler.sendNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(EnforcementPendingApplicationNotificationSent.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", equalTo(APPLICATION_ID_STR)),
                                        withJsonPath("$.sentTime", equalTo(sentAt.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldRequestEmailFailedNotification() throws EventStreamException {
        final ZonedDateTime failedTime = new UtcClock().now();

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(ENFORCEMENT_PENDING_APPLICATION_FAIL_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(APPLICATION_ID_KEY_STRING, APPLICATION_ID_STR)
                        .add("failedTime", failedTime.toString())
        );


        final EnforcementPendingApplicationNotificationFailed event = new EnforcementPendingApplicationNotificationFailed(APPLICATION_ID_UUID, failedTime);

        when(eventSource.getStreamById(APPLICATION_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, EnforcementPendingApplicationNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationFailed(APPLICATION_ID_UUID, failedTime)).thenReturn(Stream.of(event));

        handler.failNotification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(EnforcementPendingApplicationNotificationFailed.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", equalTo(APPLICATION_ID_STR)),
                                        withJsonPath("$.failedTime", equalTo(failedTime.toLocalDateTime() + "Z"))
                                ))))));
    }
}
