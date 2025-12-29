package uk.gov.moj.cpp.sjp.event.processor;


import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.CaseApplication;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerated;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementAreaEmailHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementEmailAttachmentService;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.EmailNotification;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.NotificationNotify;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class EnforcementPendingApplicationNotificationProcessor {
    private static final String QUEUE_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.enforcement-pending-application-queue-notification";
    public static final String SJP_COMMAND_UPLOAD_CASE_DOCUMENT = "sjp.command.upload-case-document";

    @Inject
    @Value(key = "enforcementPendingApplicationNotificationTemplateId", defaultValue = "07d1f043-6052-4d18-adce-58678d0e7018")
    private String templateId;

    @Inject
    @Value(key = "enforcementPendingApplicationNotificationReplyToAddress", defaultValue = "noreply@cjscp.org.uk")
    private String replyToAddress;

    @Inject
    private NotificationNotify notificationNotify;

    @Inject
    private SjpService sjpService;

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private EnforcementEmailAttachmentService enforcementPendingApplicationEmailAttachmentService;

    @Inject
    private EnforcementAreaEmailHelper enforcementAreaEmailHelper;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Handles(EnforcementPendingApplicationNotificationRequired.EVENT_NAME)
    public void initiateEmailToNotificationNotify(final JsonEnvelope envelope) throws FileServiceException {
        final EnforcementPendingApplicationNotificationRequired event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                EnforcementPendingApplicationNotificationRequired.class);
        enforcementPendingApplicationEmailAttachmentService.generateNotification(event, envelope);
    }

    @Handles(EnforcementPendingApplicationNotificationGenerated.EVENT_NAME)
    public void sendEmailToNotificationNotify(final JsonEnvelope envelope) {
        final UUID applicationId = getApplicationId(envelope);
        final CaseDetails caseDetails = sjpService.getCaseDetailsByApplicationId(getApplicationId(envelope), envelope);
        if (null == caseDetails) {
            throw new IllegalStateException("Could not find case for application id: " + applicationId.toString());
        }
        final CaseApplication caseApplication = caseDetails.getCaseApplication();
        final String postcode;
        if (nonNull(caseDetails.getDefendant().getPersonalDetails())) {
            postcode = caseDetails.getDefendant().getPersonalDetails().getAddress().getPostcode();
        } else {
            postcode = caseDetails.getDefendant().getLegalEntityDetails().getAddress().getPostcode();
        }

        final String sendToAddress = enforcementAreaEmailHelper.enforcementEmail(envelope, caseDetails, postcode);
        final ApplicationType applicationType = caseApplication.getApplicationType();
        final String emailSubject = enforcementPendingApplicationEmailAttachmentService.getEmailSubject(applicationType);
        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(getApplicationId(envelope))
                .withTemplateId(fromString(templateId))
                .withSendToAddress(sendToAddress)
                .withReplyToAddress(replyToAddress)
                .withFileId(getFileId(envelope))
                .withSubject(emailSubject)
                .build();

        systemIdMapperService.mapNotificationIdToCaseId(caseDetails.getId(), caseApplication.getApplicationId(),
                ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION);

        notificationNotify.sendEmail(emailNotification, envelope);

        sendNotificationQueuedCommand(envelope);

       uploadDocumentToCaseCommand(envelope, caseDetails.getId(), getFileId(envelope));
    }

    private void sendNotificationQueuedCommand(final JsonEnvelope envelope) {
        final String applicationId = getApplicationId(envelope).toString();
        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(QUEUE_EMAIL_NOTIFICATION_COMMAND_NAME),
                createObjectBuilder()
                        .add("applicationId", applicationId)
                        .add("queuedTime", ZonedDateTime.now().toString())
        );
        sender.send(envelopeToSend);
    }

    public void uploadDocumentToCaseCommand(final JsonEnvelope envelope, final UUID caseId, final UUID fileId) {
        final JsonEnvelope envelopeToSend = envelopeFrom(
                JsonEnvelope.metadataFrom(envelope.metadata()).withName(SJP_COMMAND_UPLOAD_CASE_DOCUMENT),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("caseDocument", fileId.toString())
                        .add("caseDocumentType", "EnforcementPendingApplicationNotification").build()
        );
        sender.send(envelopeToSend);
    }

    private UUID getApplicationId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("applicationId"));
    }

    private UUID getFileId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("fileId"));
    }
}
