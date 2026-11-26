package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class EmailNotificationRepository extends JPAEntityRepository<EmailNotification, UUID> {

    @Override
    protected Class<EmailNotification> getEntityClass() {
        return EmailNotification.class;
    }

    public EmailNotification findByReferenceIdAndNotificationType(final UUID referenceId, final EmailNotification.NotificationNotifyDocumentType notificationType) {
        return entityManager.createQuery(
                "SELECT e FROM EmailNotification e WHERE e.referenceId = :referenceId AND e.notificationType = :notificationType",
                EmailNotification.class)
                .setParameter("referenceId", referenceId)
                .setParameter("notificationType", notificationType)
                .getResultStream().findFirst().orElse(null);
    }
}
