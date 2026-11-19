package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EndorsementRemovalNotificationRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public NotificationOfEndorsementStatus findBy(final UUID id) {
        return entityManager.find(NotificationOfEndorsementStatus.class, id);
    }

    public NotificationOfEndorsementStatus save(final NotificationOfEndorsementStatus entity) {
        return entityManager.merge(entity);
    }

    public void remove(final NotificationOfEndorsementStatus entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM NotificationOfEndorsementStatus e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<NotificationOfEndorsementStatus> findAll() {
        return entityManager.createQuery("SELECT e FROM NotificationOfEndorsementStatus e", NotificationOfEndorsementStatus.class).getResultList();
    }
}
