package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class PleaNotificationProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    //TODO: move to configuration
    static final String TEMPLATE_ID = "32d520ca-4d6e-4b5c-a9f3-e761d4ffd9a2";
    static final String REPLY_TO_ADDRESS = "noreply@cjscp.org.uk";

    @Handles("sjp.events.online-plea-received")
    public void sendPleaNotificationEmail(final JsonEnvelope envelope) {
        final OnlinePleaReceived onlinePleaReceived = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), OnlinePleaReceived.class);

        final JsonObject emailNotification = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("templateId", TEMPLATE_ID)
                .add("sendToAddress", onlinePleaReceived.getPersonalDetails().getContactDetails().getEmail())
                .add("replyToAddress", REPLY_TO_ADDRESS)
                .add("personalisation", createObjectBuilder()
                    .add("urn", onlinePleaReceived.getUrn())
                    .build())
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, "notificationnotify.send-email-notification")
                .apply(emailNotification));
    }
}
