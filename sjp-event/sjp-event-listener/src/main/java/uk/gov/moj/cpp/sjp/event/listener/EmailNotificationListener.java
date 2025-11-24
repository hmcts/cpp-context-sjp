package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorFailed;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorQueued;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorSent;
import uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification;
import uk.gov.moj.cpp.sjp.persistence.repository.EmailNotificationRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class EmailNotificationListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private EmailNotificationRepository repository;

    @Transactional
    @Handles(PartialAocpCriteriaNotificationProsecutorQueued.EVENT_NAME)
    public void notificationQueued(final JsonEnvelope envelope) {
        final PartialAocpCriteriaNotificationProsecutorQueued event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                PartialAocpCriteriaNotificationProsecutorQueued.class);

        final EmailNotification entity = new EmailNotification();
        entity.setId(UUID.randomUUID());
        entity.setReferenceId(event.getCaseId());
        entity.setStatus(EmailNotification.Status.QUEUED);
        entity.setUpdated(ZonedDateTime.now());
        entity.setNotificationType(PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        repository.save(entity);
    }

    @Transactional
    @Handles(PartialAocpCriteriaNotificationProsecutorFailed.EVENT_NAME)
    public void notificationFailed(final JsonEnvelope envelope) {
        final PartialAocpCriteriaNotificationProsecutorFailed event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                PartialAocpCriteriaNotificationProsecutorFailed.class);

        final EmailNotification entity = repository.findByReferenceIdAndNotificationType(event.getCaseId(), PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        entity.setStatus(EmailNotification.Status.FAILED);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }

    @Transactional
    @Handles(PartialAocpCriteriaNotificationProsecutorSent.EVENT_NAME)
    public void notificationSent(final JsonEnvelope envelope) {
        final PartialAocpCriteriaNotificationProsecutorSent event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                PartialAocpCriteriaNotificationProsecutorSent.class);
        final EmailNotification entity = repository.findByReferenceIdAndNotificationType(event.getCaseId(), PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        entity.setStatus(EmailNotification.Status.SENT);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }
}
