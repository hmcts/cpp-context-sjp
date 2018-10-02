package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.processor.service.Country;
import uk.gov.moj.cpp.sjp.event.processor.service.PostcodeService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

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

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private PostcodeService postcodeService;

    @Inject
    @Value(key = "pleaNotificationEnglishTemplateId", defaultValue = "07d1f043-6052-4d18-adce-58678d0e7018")
    String englishTemplateId;

    @Inject
    @Value(key = "pleaNotificationWelshTemplateId", defaultValue = "af48b904-3ee5-402b-bd82-0a80249c4405")
    String welshTemplateId;

    @Inject
    @Value(key = "pleaNotificationReplyToAddress", defaultValue = "noreply@cjscp.org.uk")
    String replyToAddress;

    @Handles("sjp.events.online-plea-received")
    public void sendPleaNotificationEmail(final JsonEnvelope envelope) {
        final OnlinePleaReceived onlinePleaReceived = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), OnlinePleaReceived.class);

        final JsonObject emailNotification = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("templateId", getTemplateId(onlinePleaReceived.getPersonalDetails().getAddress().getPostcode(), envelope))
                .add("sendToAddress", onlinePleaReceived.getPersonalDetails().getContactDetails().getEmail())
                .add("replyToAddress", replyToAddress)
                .add("personalisation", createObjectBuilder()
                        .add("urn", onlinePleaReceived.getUrn())
                        .build())
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, "notificationnotify.send-email-notification")
                .apply(emailNotification));
    }

    private String getTemplateId(String postcode, JsonEnvelope envelope) {
        final String outwardCode = postcodeService.getOutwardCode(postcode);
        final String country = referenceDataService.getCountryByPostcode(outwardCode, envelope);
        return Country.WALES.getName().equalsIgnoreCase(country) ? welshTemplateId : englishTemplateId;
    }

}
