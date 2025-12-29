package uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

public class NotificationNotify {
    private static final String COMMAND_NAME = "notificationnotify.send-email-notification";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    public void sendEmail(final EmailNotification emailNotification, final JsonEnvelope envelope) {

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", emailNotification.getNotificationId().toString())
                .add("templateId", emailNotification.getTemplateId().toString())
                .add("sendToAddress", emailNotification.getSendToAddress())
                .add("replyToAddress", emailNotification.getReplyToAddress());

        if(emailNotification.getFileId() != null){
            payload.add("fileId", emailNotification.getFileId().toString());
        }

        addSubject(emailNotification, payload);

        final JsonEnvelope envelopeToSend = envelopeFrom(
                JsonEnvelope.metadataFrom(envelope.metadata()).withName(COMMAND_NAME), payload);
        sender.sendAsAdmin(envelopeToSend);
    }

    private void addSubject(final EmailNotification emailNotification, final JsonObjectBuilder payload) {
        emailNotification.getSubject().ifPresent(subject ->
                payload.add("personalisation", createObjectBuilder().add("subject", subject)));
    }
}
