package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EmailNotificationRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public EmailNotification findByReferenceIdAndNotificationType(final UUID referenceId, final EmailNotification.NotificationNotifyDocumentType notificationType) {
        return entityManager.createQuery(
                "SELECT e FROM EmailNotification e WHERE e.referenceId = :referenceId AND e.notificationType = :notificationType",
                EmailNotification.class)
                .setParameter("referenceId", referenceId)
                .setParameter("notificationType", notificationType)
                .getResultStream().findFirst().orElse(null);
    }

    public EmailNotification findBy(final UUID id) {
        return entityManager.find(EmailNotification.class, id);
    }

    public EmailNotification save(final EmailNotification entity) {
        return entityManager.merge(entity);
    }

    public void remove(final EmailNotification entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM EmailNotification e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<EmailNotification> findAll() {
        return entityManager.createQuery("SELECT e FROM EmailNotification e", EmailNotification.class).getResultList();
    }
}
