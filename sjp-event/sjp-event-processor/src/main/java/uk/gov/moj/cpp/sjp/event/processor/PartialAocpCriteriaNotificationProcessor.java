package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationService;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.EmailNotification;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.NotificationNotify;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class PartialAocpCriteriaNotificationProcessor {

    private static final String COMMAND_NAME = "sjp.command.update-partial-aocp-criteria-notification-to-prosecutor-status";

    @Inject
    @Value(key = "notificationOfFollowSJPTemplateId", defaultValue = "3752de75-7ab4-4c4f-8a01-2a72aa1ea63d")
    String templateId;

    @Inject
    @Value(key = "notificationOfFollowSJPReplyToAddress", defaultValue = "SIS@justice.gov.uk")
    String replyToAddress;

    @Inject
    @Value(key = "notificationOfFollowSJPSubject", defaultValue = "Your case will follow the Single Justice Procedure (SJP)")
    private String subject;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private NotificationNotify notificationNotify;

    @Inject
    private SjpService sjpService;

    @Inject
    private EndorsementRemovalNotificationService endorsementRemovalNotificationService;

    @Inject
    private Sender sender;

    @Inject
    private SystemIdMapperService systemIdMapperService;


    @Handles("sjp.events.aocp-criteria-matched-partially")
    public void sendEmailToNotificationNotify(final JsonEnvelope envelope) {

        final String caseId = envelope.payloadAsJsonObject().getString("caseId");
        final String prosecutingAuthority = envelope.payloadAsJsonObject().getString("prosecutingAuthority");
        final Optional<JsonObject> prosecutorDetails = referenceDataService.getProsecutor(prosecutingAuthority, envelope);
        Optional<String> sendToAddressOpt = Optional.empty();

        if (prosecutorDetails.isPresent()) {
            sendToAddressOpt = getString(prosecutorDetails.get(), "contactEmailAddress");
        }

        final String sendToAddress = sendToAddressOpt.orElseThrow(() ->
                new IllegalStateException("Unable to find prosecutor email address in reference data"));

        final UUID notificationCaseId = fromString(caseId);
        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(notificationCaseId)
                .withTemplateId(fromString(templateId))
                .withSendToAddress(sendToAddress)
                .withReplyToAddress(replyToAddress)
                .withSubject(subject)
                .build();

        systemIdMapperService.mapNotificationIdToCaseId(notificationCaseId, notificationCaseId, PARTIAL_AOCP_CRITERIA_NOTIFICATION);

        notificationNotify.sendEmail(emailNotification, envelope);

        sendNotificationQueuedCommand(envelope, notificationCaseId.toString());
    }

    private void  sendNotificationQueuedCommand(final JsonEnvelope envelope, final String caseId) {
        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_NAME),
                createObjectBuilder().add("caseId", caseId));
        sender.send(envelopeToSend);
    }
}
