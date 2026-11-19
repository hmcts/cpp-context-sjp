package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EnforcementPendingApplicationNotificationStatusRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public EnforcementNotification findBy(final UUID id) {
        return entityManager.find(EnforcementNotification.class, id);
    }

    public EnforcementNotification save(final EnforcementNotification entity) {
        return entityManager.merge(entity);
    }

    public void remove(final EnforcementNotification entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM EnforcementNotification e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<EnforcementNotification> findAll() {
        return entityManager.createQuery("SELECT e FROM EnforcementNotification e", EnforcementNotification.class).getResultList();
    }
}
