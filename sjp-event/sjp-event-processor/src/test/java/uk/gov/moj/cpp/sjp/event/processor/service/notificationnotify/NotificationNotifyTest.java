package uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationNotifyTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private NotificationNotify notificationNotify;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendData() {
        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(randomUUID())
                .withTemplateId(randomUUID())
                .withSendToAddress("sendToAddress")
                .withReplyToAddress("replyToAddress")
                .withFileId(randomUUID())
                .withSubject("my subject")
                .build();

        notificationNotify.sendEmail(emailNotification, envelope());

        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        final JsonEnvelope emailCommand = envelopeCaptor.getValue();
        assertThat(emailCommand.metadata().name(), is("notificationnotify.send-email-notification"));
        final JsonObject payload = emailCommand.payloadAsJsonObject();
        assertThat(payload.size(), is(6));
        assertThat(payload.getString("notificationId"), equalTo(emailNotification.getNotificationId().toString()));
        assertThat(payload.getString("templateId"), equalTo(emailNotification.getTemplateId().toString()));
        assertThat(payload.getString("sendToAddress"), equalTo(emailNotification.getSendToAddress()));
        assertThat(payload.getString("replyToAddress"), equalTo(emailNotification.getReplyToAddress()));
        assertThat(payload.getString("fileId"), equalTo(emailNotification.getFileId().toString()));
        assertThat(payload.getJsonObject("personalisation"), notNullValue());
        assertThat(payload.getJsonObject("personalisation").getString("subject"), equalTo("my subject"));
    }

    @Test
    public void subjectShouldBeOptional() {
        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(randomUUID())
                .withTemplateId(randomUUID())
                .withSendToAddress("sendToAddress")
                .withReplyToAddress("replyToAddress")
                .withFileId(randomUUID())
                .build();

        notificationNotify.sendEmail(emailNotification, envelope());

        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        final JsonEnvelope emailCommand = envelopeCaptor.getValue();
        assertThat(emailCommand.payloadAsJsonObject().size(), is(5));
        assertThat(emailCommand.payloadAsJsonObject().getJsonObject("personalisation"), nullValue());
    }

    private JsonEnvelope envelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("origin envelope"),
                createObjectBuilder()
        );
    }
}