package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.AOCP_ACCEPTED_EMAIL_NOTIFICATION;
import static java.time.LocalDate.now;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import javax.inject.Inject;
import javax.json.JsonObject;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.DefendantAcceptedAocp;
import uk.gov.moj.cpp.sjp.event.processor.service.Country;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantAcceptedAocpProcessor {

    private static final String COMMAND_NAME = "sjp.command.update-aocp-acceptance-email-notification";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private TimerService timerService;

    @Inject
    @Value(key = "aopcPleaNotificationEnglishTemplateId", defaultValue = "2300fded-e52f-4564-a92a-a6412b1c0f09")
    String englishTemplateId;

    @Inject
    @Value(key = "aopcPleaNotificationWelshTemplateId", defaultValue = "b327f28b-010d-47f9-954d-f21a4ee9ddfc")
    String welshTemplateId;

    @Inject
    @Value(key = "pleaNotificationReplyToAddress", defaultValue = "noreply@cjscp.org.uk")
    String replyToAddress;

    @Handles("sjp.events.defendant-accepted-aocp")
    public void sendPleaNotificationEmail(final JsonEnvelope envelope) {
        final DefendantAcceptedAocp defendantAcceptedAocp = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantAcceptedAocp.class);
        final JsonObject emailNotification = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("templateId", getTemplateId(defendantAcceptedAocp.getPersonalDetails().getAddress().getPostcode(), envelope))
                .add("sendToAddress", defendantAcceptedAocp.getPersonalDetails().getContactDetails().getEmail())
                .add("replyToAddress", replyToAddress)
                .add("personalisation", createObjectBuilder()
                        .add("urn", defendantAcceptedAocp.getCaseUrn())
                        .build())
                .build();

        systemIdMapperService.mapNotificationIdToCaseId(defendantAcceptedAocp.getCaseId(), defendantAcceptedAocp.getCaseId(), AOCP_ACCEPTED_EMAIL_NOTIFICATION);

        sender.send(enveloper.withMetadataFrom(envelope, "notificationnotify.send-email-notification")
                .apply(emailNotification));

        sendNotificationQueuedCommand(envelope, defendantAcceptedAocp.getCaseId().toString());

        final LocalDate defendantAocpAcceptanceExpiryDate = now().plusDays(5);

        final Metadata metaData = metadataFrom(envelope.metadata()).withUserId(systemIdMapperService.getSystemUserId().toString()).build();

        timerService.startTimerForDefendantAOCPAcceptance(defendantAcceptedAocp.getCaseId(), defendantAocpAcceptanceExpiryDate, metaData);
    }

    private String getTemplateId(String postcode, JsonEnvelope envelope) {
        final String country = referenceDataService.getCountryByPostcode(postcode, envelope);
        return Country.WALES.getName().equalsIgnoreCase(country) ? welshTemplateId : englishTemplateId;
    }

    private void  sendNotificationQueuedCommand(final JsonEnvelope envelope, final String caseId) {
        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_NAME),
                createObjectBuilder().add("caseId", caseId).add("queuedTime", ZonedDateTime.now().toString()));
        sender.send(envelopeToSend);
    }
}
