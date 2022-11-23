package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
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


import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.AocpAcceptedEmailNotificationAggregate;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationFailed;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationQueued;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationSent;


@RunWith(MockitoJUnitRunner.class)
public class AocpAcceptedEmailNotificationHandlerTest {

    public static final String AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND = "sjp.command.update-aocp-acceptance-email-notification";

    @InjectMocks
    private AocpAcceptedEmailNotificationHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private AocpAcceptedEmailNotificationAggregate aggregate;

    private final String CASE_ID_KEY_STRING = "caseId";
    private final UUID CASE_ID_UUID = UUID.randomUUID();
    private final String CASE_ID_STR = CASE_ID_UUID.toString();

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            AocpAcceptedEmailNotificationQueued.class,
            AocpAcceptedEmailNotificationSent.class,
            AocpAcceptedEmailNotificationFailed.class);



    @Test
    public void shouldHandleQueueNotificationCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("notification")
                        .thatHandles(AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND)
                ));
    }

    @Test
    public void shouldRequestEmailQueuedNotification() throws EventStreamException {
        final ZonedDateTime queuedTime = ZonedDateTime.now();
        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(CASE_ID_KEY_STRING, CASE_ID_STR)
                        .add("queuedTime", queuedTime.toString())
        );

        final AocpAcceptedEmailNotificationQueued event = new AocpAcceptedEmailNotificationQueued(CASE_ID_UUID, queuedTime);

        when(eventSource.getStreamById(CASE_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, AocpAcceptedEmailNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationQueued(CASE_ID_UUID, queuedTime)).thenReturn(Stream.of(event));

        handler.notification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(AocpAcceptedEmailNotificationQueued.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID_STR)),
                                        withJsonPath("$.queuedTime", equalTo(queuedTime.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldRequestEmailSendNotification() throws EventStreamException {
        final ZonedDateTime sentAt = ZonedDateTime.now();

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(CASE_ID_KEY_STRING, CASE_ID_STR)
                        .add("sentTime", sentAt.toString())
        );


        final AocpAcceptedEmailNotificationSent event = new AocpAcceptedEmailNotificationSent(CASE_ID_UUID, sentAt);

        when(eventSource.getStreamById(CASE_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, AocpAcceptedEmailNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationSent(CASE_ID_UUID, sentAt)).thenReturn(Stream.of(event));

        handler.notification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(AocpAcceptedEmailNotificationSent.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID_STR)),
                                        withJsonPath("$.sentTime", equalTo(sentAt.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldRequestEmailFailedNotification() throws EventStreamException {
        final ZonedDateTime failedTime = ZonedDateTime.now();

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataWithRandomUUID(AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add(CASE_ID_KEY_STRING, CASE_ID_STR)
                        .add("failedTime", failedTime.toString())
        );


        final AocpAcceptedEmailNotificationFailed event = new AocpAcceptedEmailNotificationFailed(CASE_ID_UUID, failedTime);

        when(eventSource.getStreamById(CASE_ID_UUID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, AocpAcceptedEmailNotificationAggregate.class)).thenReturn(aggregate);
        when(aggregate.markAsNotificationFailed(CASE_ID_UUID, failedTime)).thenReturn(Stream.of(event));

        handler.notification(commandEnvelope);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(commandEnvelope)
                                        .withName(AocpAcceptedEmailNotificationFailed.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID_STR)),
                                        withJsonPath("$.failedTime", equalTo(failedTime.toLocalDateTime() + "Z"))
                                ))))));
    }
}
