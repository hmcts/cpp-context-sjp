package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.ENDORSEMENT_REMOVAL_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.EnforcementPendingApplicationNotificationStatus;
import uk.gov.moj.cpp.sjp.event.processor.service.NotificationOfEndorsementStatus;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationNotifyProcessorTest {
    private static final String FAIL_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.enforcement-pending-application-fail-notification";
    private static final String SEND_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.enforcement-pending-application-send-notification";
    private static final String FAIL_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.endorsement-removal-notification-failed";
    private static final String SEND_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.endorsement-removal-notification-sent";

    private static final String NOTIFICATION_TIME = "2016-07-11T12:55:28.180Z";

    @Mock
    private Sender sender;
    @Mock
    private SjpService sjpService;
    @Mock
    private SystemIdMapperService systemIdMapperService;
    @Mock
    private SystemIdMapping systemIdMapping;

    @InjectMocks
    private NotificationNotifyProcessor processor;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private UUID notificationId = randomUUID();


    @Test
    public void shouldSendFailedCommandWhenNotificationFailedIsReceivedFromNotificationNotify() {

        final JsonEnvelope envelope = notificationFailedEnvelope(notificationId.toString());
        givenNotificationIdIsPresentInViewstore(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENDORSEMENT_REMOVAL_NOTIFICATION.name());

        processor.notificationFailed(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommand = envelopeCaptor.getValue();
        assertThat(sentCommand.metadata().name(), equalTo(FAIL_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME));
        assertThat(sentCommand.payloadAsJsonObject().getString("applicationDecisionId"), equalTo(notificationId.toString()));
        assertThat(sentCommand.payloadAsJsonObject().getString("failedTime"), equalTo(NOTIFICATION_TIME));
    }

    @Test
    public void shouldSendSentCommandWhenNotificationSentIsReceivedFromNotificationNotify() {
        final JsonEnvelope envelope = notificationSentEnvelope(notificationId.toString());
        givenNotificationIdIsPresentInViewstore(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENDORSEMENT_REMOVAL_NOTIFICATION.name());

        processor.notificationSent(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommand = envelopeCaptor.getValue();
        assertThat(sentCommand.metadata().name(), equalTo(SEND_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME));
        assertThat(sentCommand.payloadAsJsonObject().getString("applicationDecisionId"), equalTo(notificationId.toString()));
        assertThat(sentCommand.payloadAsJsonObject().getString("sentTime"), equalTo(NOTIFICATION_TIME));
    }

    @Test
    public void shouldIgnoreMessagesNotificationFailedEventWhenNotificationIdIsNotPresentInTheViewstore() {
        final JsonEnvelope envelope = notificationFailedEnvelope(notificationId.toString());
        givenNotificationIdIsNotPresentInViewstore(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENDORSEMENT_REMOVAL_NOTIFICATION.name());
        processor.notificationFailed(envelope);

        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldIgnoreMessagesNotificationSentEventWhenNotificationIdIsNotPresentInTheViewstore() {
        final JsonEnvelope envelope = notificationSentEnvelope(notificationId.toString());
        givenNotificationIdIsNotPresentInViewstore(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENDORSEMENT_REMOVAL_NOTIFICATION.name());

        processor.notificationSent(envelope);

        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldSendFailedCommandWhenNotificationFailedIsReceivedFromNotificationNotifyForEnforcementPendingApplications() {
        final JsonEnvelope envelope = notificationFailedEnvelope(notificationId.toString());
        givenNotificationIdIsPresentInViewstoreForEnforcementPendingApplications(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name());

        processor.notificationFailed(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommand = envelopeCaptor.getValue();
        assertThat(sentCommand.metadata().name(), equalTo(FAIL_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME));
        assertThat(sentCommand.payloadAsJsonObject().getString("applicationId"), equalTo(notificationId.toString()));
        assertThat(sentCommand.payloadAsJsonObject().getString("failedTime"), equalTo(NOTIFICATION_TIME));
    }

    @Test
    public void shouldSendSentCommandWhenNotificationSentIsReceivedFromNotificationNotifyForEnforcementPendingApplications() {
        final JsonEnvelope envelope = notificationSentEnvelope(notificationId.toString());
        givenNotificationIdIsPresentInViewstoreForEnforcementPendingApplications(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name());

        processor.notificationSent(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommand = envelopeCaptor.getValue();
        assertThat(sentCommand.metadata().name(), equalTo(SEND_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME));
        assertThat(sentCommand.payloadAsJsonObject().getString("applicationId"), equalTo(notificationId.toString()));
        assertThat(sentCommand.payloadAsJsonObject().getString("sentTime"), equalTo(NOTIFICATION_TIME));
    }

    @Test
    public void shouldIgnoreMessagesNotificationFailedEventWhenNotificationIdIsNotPresentInTheViewstoreForEnforcementPendingApplications() {
        final JsonEnvelope envelope = notificationFailedEnvelope(notificationId.toString());
        givenNotificationIdIsNotPresentInViewstoreForEnforcementPendingApplications(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name());

        processor.notificationFailed(envelope);
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldIgnoreMessagesNotificationSentEventWhenNotificationIdIsNotPresentInTheViewstoreForEnforcementPendingApplications() {
        final JsonEnvelope envelope = notificationSentEnvelope(notificationId.toString());
        givenNotificationIdIsNotPresentInViewstoreForEnforcementPendingApplications(envelope);
        when(systemIdMapperService.getSystemIdMappingForNotificationId(notificationId)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getSourceType()).thenReturn(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name());

        processor.notificationSent(envelope);

        verify(sender, never()).send(envelopeCaptor.capture());
    }

    private JsonEnvelope notificationSentEnvelope(final String notificationId) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", notificationId)
                .add("sentTime", NOTIFICATION_TIME);

        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("public.notificationnotify.events.notification-sent")
                        .build(),
                payload.build()
        );
    }

    private JsonEnvelope notificationFailedEnvelope(final String notificationId) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", notificationId)
                .add("failedTime", NOTIFICATION_TIME);

        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("public.notificationnotify.events.notification-failed")
                        .build(),
                payload.build()
        );
    }

    private void givenNotificationIdIsPresentInViewstore(final JsonEnvelope envelope) {
        final NotificationOfEndorsementStatus currentStatus = NotificationOfEndorsementStatus.queued(notificationId);
        when(sjpService.getNotificationOfEndorsementStatus(notificationId, envelope)).thenReturn(of(currentStatus));
    }

    private void givenNotificationIdIsNotPresentInViewstore(final JsonEnvelope envelope) {
        when(sjpService.getNotificationOfEndorsementStatus(notificationId, envelope)).thenReturn(Optional.empty());
    }

    private void givenNotificationIdIsPresentInViewstoreForEnforcementPendingApplications(final JsonEnvelope envelope) {
        final EnforcementPendingApplicationNotificationStatus currentStatus = EnforcementPendingApplicationNotificationStatus.queued(notificationId);
        when(sjpService.getEnforcementPendingApplicationNotificationStatus(notificationId, envelope)).thenReturn(of(currentStatus));
    }

    private void givenNotificationIdIsNotPresentInViewstoreForEnforcementPendingApplications(final JsonEnvelope envelope) {
        when(sjpService.getEnforcementPendingApplicationNotificationStatus(notificationId, envelope)).thenReturn(Optional.empty());
    }
}