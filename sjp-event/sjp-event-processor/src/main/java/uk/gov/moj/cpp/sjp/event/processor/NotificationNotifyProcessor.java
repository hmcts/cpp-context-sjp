package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.AOCP_ACCEPTED_EMAIL_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.ENDORSEMENT_REMOVAL_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.AocpAcceptedEmailNotificationStatus;
import uk.gov.moj.cpp.sjp.event.processor.service.EnforcementPendingApplicationNotificationStatus;
import uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType;
import uk.gov.moj.cpp.sjp.event.processor.service.NotificationOfEndorsementStatus;
import uk.gov.moj.cpp.sjp.event.processor.service.NotificationOfPartialAocpStatus;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

/**
 * Process responses from Notification.Notify public events for sent and failed emails
 * <p>
 * Currently this processor only processes the response for Notification to DVLA to Remove
 * Endorsements. Should the scope of this class increase handling more business cases/notifications
 * we should move the behaviour for each to their own dedicated classes in order not to mix
 * concerns
 */
@ServiceComponent(EVENT_PROCESSOR)
public class NotificationNotifyProcessor {

    private static final String FAIL_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.enforcement-pending-application-fail-notification";
    private static final String SEND_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.enforcement-pending-application-send-notification";
    private static final String FAIL_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.endorsement-removal-notification-failed";
    private static final String SEND_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.endorsement-removal-notification-sent";

    private static final String SENT_TIME = "sentTime";
    private static final String FAILED_TIME = "failedTime";
    private static final String AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.update-aocp-acceptance-email-notification";
    private static final String PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND_NAME = "sjp.command.update-partial-aocp-criteria-notification-to-prosecutor-status";

    @Inject
    private SjpService sjpService;

    @Inject
    private Sender sender;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Handles("public.notificationnotify.events.notification-sent")
    public void notificationSent(final JsonEnvelope envelope) {
        final UUID notificationId = getNotificationId(envelope);
        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperService.getSystemIdMappingForNotificationId(notificationId);
        if (systemIdMapping.isPresent()) {
            final String sourceType = systemIdMapping.get().getSourceType();
            final NotificationNotifyDocumentType documentType = NotificationNotifyDocumentType.fromString(sourceType);
            if (documentType.equals(ENDORSEMENT_REMOVAL_NOTIFICATION)) {
                sendEndorsementRemovalNotification(envelope, getSentTime(envelope));
            }
            if (documentType.equals(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION)) {
                sendEnforcementPendingNotification(envelope, getSentTime(envelope));
            }
            if(documentType.equals(PARTIAL_AOCP_CRITERIA_NOTIFICATION)){
                sendPartialAocpNotification(envelope, getSentTime(envelope));
            }
            if (documentType.equals(AOCP_ACCEPTED_EMAIL_NOTIFICATION)) {
                sendAocpAcceptedEmailNotification(envelope, getSentTime(envelope));
            }
        }
    }

    @Handles("public.notificationnotify.events.notification-failed")
    public void notificationFailed(final JsonEnvelope envelope) {
        final UUID notificationId = getNotificationId(envelope);
        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperService.getSystemIdMappingForNotificationId(notificationId);
        if (systemIdMapping.isPresent()) {
            final String sourceType = systemIdMapping.get().getSourceType();
            final NotificationNotifyDocumentType documentType = NotificationNotifyDocumentType.fromString(sourceType);
            if (documentType.equals(ENDORSEMENT_REMOVAL_NOTIFICATION)) {
                failEndorsementRemovalNotification(envelope, getFailedTime(envelope));
            }
            if (documentType.equals(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION)) {
                failEnforcementPendingNotification(envelope, getFailedTime(envelope));
            }
            if(documentType.equals(PARTIAL_AOCP_CRITERIA_NOTIFICATION)){
                failPartialAocpNotification(envelope, getFailedTime(envelope));
            }
            if (documentType.equals(AOCP_ACCEPTED_EMAIL_NOTIFICATION)) {
                failAocpAcceptedEmailNotification(envelope, getFailedTime(envelope));
            }
        }
    }

    private void sendEndorsementRemovalNotification(final JsonEnvelope envelope,
                                                    final ZonedDateTime sentTime) {
        final UUID applicationDecisionId = getNotificationId(envelope);

        final Optional<NotificationOfEndorsementStatus> notificationOfEndorsementStatus =
                sjpService.getNotificationOfEndorsementStatus(applicationDecisionId, envelope);

        notificationOfEndorsementStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(SEND_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add("applicationDecisionId", applicationDecisionId.toString())
                                .add(SENT_TIME, sentTime.toString())
                                .build())));
    }

    private void sendPartialAocpNotification(final JsonEnvelope envelope,
                                             final ZonedDateTime sentTime) {

        final UUID caseId = getNotificationId(envelope);

        final Optional<NotificationOfPartialAocpStatus> notificationOfPartialAocpStatus =
                sjpService.getNotificationOfPartialAocpStatus(caseId, envelope);

        notificationOfPartialAocpStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add(CASE_ID, caseId.toString())
                                .add(SENT_TIME, sentTime.toString())
                                .build())));

    }

    private void failEndorsementRemovalNotification(JsonEnvelope envelope,
                                                    final ZonedDateTime failedTime) {
        final UUID applicationDecisionId = getNotificationId(envelope);

        final Optional<NotificationOfEndorsementStatus> notificationOfEndorsementStatus =
                sjpService.getNotificationOfEndorsementStatus(applicationDecisionId, envelope);

        notificationOfEndorsementStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(FAIL_ENDORCEMENT_REMOVAL_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add("applicationDecisionId", applicationDecisionId.toString())
                                .add(FAILED_TIME, failedTime.toString())
                                .build()
                ))
        );
    }

    private void failPartialAocpNotification(JsonEnvelope envelope,
                                             final ZonedDateTime failedTime) {
        final UUID caseId = getNotificationId(envelope);

        final Optional<NotificationOfPartialAocpStatus> notificationOfPartialAocpStatus =
                sjpService.getNotificationOfPartialAocpStatus(caseId, envelope);

        notificationOfPartialAocpStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(PARTIAL_AOCP_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add(CASE_ID, caseId.toString())
                                .add(FAILED_TIME, failedTime.toString())
                                .build()
                ))
        );

    }

    private void sendEnforcementPendingNotification(final JsonEnvelope envelope,
                                                    final ZonedDateTime sentTime) {
        final UUID applicationId = getNotificationId(envelope);

        final Optional<EnforcementPendingApplicationNotificationStatus> enforcementPendingApplicationNotificationStatus =
                sjpService.getEnforcementPendingApplicationNotificationStatus(applicationId, envelope);

        enforcementPendingApplicationNotificationStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(SEND_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add("applicationId", applicationId.toString())
                                .add(SENT_TIME, sentTime.toString())
                                .build()
                ))
        );
    }

    private void failEnforcementPendingNotification(JsonEnvelope envelope,
                                                    final ZonedDateTime failedTime) {
        final UUID applicationId = getNotificationId(envelope);

        final Optional<EnforcementPendingApplicationNotificationStatus> enforcementPendingApplicationNotificationStatus =
                sjpService.getEnforcementPendingApplicationNotificationStatus(applicationId, envelope);

        enforcementPendingApplicationNotificationStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(FAIL_ENFORCEMENT_PENDING_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add("applicationId", applicationId.toString())
                                .add(FAILED_TIME, failedTime.toString())
                                .build()
                ))
        );
    }

    private void sendAocpAcceptedEmailNotification(final JsonEnvelope envelope,
                                                    final ZonedDateTime sentTime) {
        final UUID caseId = getNotificationId(envelope);

        final Optional<AocpAcceptedEmailNotificationStatus> aocpAcceptedEmailNotificationStatus =
                sjpService.getAocpAcceptedEmailNotificationStatus(caseId, envelope);

        aocpAcceptedEmailNotificationStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add(CASE_ID, caseId.toString())
                                .add(SENT_TIME, sentTime.toString())
                                .build()
                ))
        );
    }

    private void failAocpAcceptedEmailNotification(JsonEnvelope envelope,
                                                    final ZonedDateTime failedTime) {
        final UUID caseId = getNotificationId(envelope);

        final Optional<AocpAcceptedEmailNotificationStatus> aocpAcceptedEmailNotificationStatus =
                sjpService.getAocpAcceptedEmailNotificationStatus(caseId, envelope);

        aocpAcceptedEmailNotificationStatus.ifPresent(value ->
                sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName(AOCP_ACCEPTED_EMAIL_NOTIFICATION_COMMAND_NAME)
                                .build(),
                        createObjectBuilder()
                                .add(CASE_ID, caseId.toString())
                                .add(FAILED_TIME, failedTime.toString())
                                .build()
                ))
        );
    }

    private ZonedDateTime getSentTime(final JsonEnvelope envelope) {
        return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString(SENT_TIME));
    }

    private ZonedDateTime getFailedTime(final JsonEnvelope envelope) {
        return ZonedDateTime.parse(envelope.payloadAsJsonObject().getString(FAILED_TIME));
    }

    private UUID getNotificationId(final JsonEnvelope envelope) {
        return fromString(envelope.payloadAsJsonObject().getString("notificationId"));
    }
}
