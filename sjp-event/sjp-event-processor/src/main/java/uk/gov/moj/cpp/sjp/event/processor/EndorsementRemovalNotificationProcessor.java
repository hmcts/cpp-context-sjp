package uk.gov.moj.cpp.sjp.event.processor;


import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.ENDORSEMENT_REMOVAL_NOTIFICATION;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerated;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.models.ApplicationDecisionDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationService;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.EmailNotification;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.NotificationNotify;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class EndorsementRemovalNotificationProcessor {

    private static final String COMMAND_NAME = "sjp.command.endorsement-removal-notification-queued";
    public static final String SJP_COMMAND_UPLOAD_CASE_DOCUMENT = "sjp.command.upload-case-document";

    @Inject
    @Value(key = "notificationOfEndorsementsTemplateId")
    private String templateId;
    @Inject
    @Value(key = "pleaNotificationReplyToAddress", defaultValue = "noreply@cjscp.org.uk")
    private String replyToAddress;
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


    @Handles(NotificationToRemoveEndorsementsGenerated.EVENT_NAME)
    public void sendEmailToNotificationNotify(final JsonEnvelope envelope) {
        final Optional<String> dvlaEmailAddress = referenceDataService.getDvlaPenaltyPointNotificationEmailAddress(envelope);

        final String sendToAddress = dvlaEmailAddress.orElseThrow(() ->
                new IllegalStateException("Unable to find DVLA email address in reference data"));

        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(applicationDecisionId)
                .withTemplateId(fromString(templateId))
                .withSendToAddress(sendToAddress)
                .withReplyToAddress(replyToAddress)
                .withFileId(getFileId(envelope))
                .withSubject(buildSubject(envelope))
                .build();

        systemIdMapperService.mapNotificationIdToCaseId(applicationDecisionId, applicationDecisionId, ENDORSEMENT_REMOVAL_NOTIFICATION);

        notificationNotify.sendEmail(emailNotification, envelope);

        sendNotificationQueuedCommand(envelope);

        uploadDocumentToCaseCommand(envelope, getCaseDetails(envelope).getId(), getFileId(envelope));
    }

    public void uploadDocumentToCaseCommand(final JsonEnvelope envelope, final UUID caseId, final UUID fileId) {
        final JsonEnvelope envelopeToSend = envelopeFrom(
                JsonEnvelope.metadataFrom(envelope.metadata()).withName(SJP_COMMAND_UPLOAD_CASE_DOCUMENT),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("caseDocument", fileId.toString())
                        .add("caseDocumentType", "EndorsementRemovalNotification").build()
        );
        sender.send(envelopeToSend);
    }

    private void  sendNotificationQueuedCommand(final JsonEnvelope envelope) {
        final String applicationDecisionId = getApplicationDecisionId(envelope).toString();
        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_NAME),
                createObjectBuilder().add("applicationDecisionId", applicationDecisionId));
        sender.send(envelopeToSend);
    }

    private String buildSubject(final JsonEnvelope envelope) {
        final ApplicationDecisionDecorator applicationDecision = getCaseDetails(envelope)
                .getApplicationDecisionByDecisionId(getApplicationDecisionId(envelope))
                .orElseThrow(IllegalStateException::new);

        return endorsementRemovalNotificationService.buildEmailSubject(applicationDecision, envelope);
    }

    private CaseDetailsDecorator getCaseDetails(final JsonEnvelope envelope) {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        final Optional<CaseDetailsDecorator> caseDetails = sjpService.getCaseDetailsByApplicationDecisionId(applicationDecisionId, envelope);
        return caseDetails.orElseThrow(() ->
                new IllegalStateException("Could not find case for application decision id: " + applicationDecisionId.toString()));
    }

    private UUID getApplicationDecisionId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("applicationDecisionId"));
    }

    private UUID getFileId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("fileId"));
    }
}
