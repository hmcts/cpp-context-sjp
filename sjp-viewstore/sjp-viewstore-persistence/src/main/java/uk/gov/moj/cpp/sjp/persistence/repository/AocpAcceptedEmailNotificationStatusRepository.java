package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AocpAcceptedEmailNotificationStatusRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public AocpAcceptedEmailStatus findBy(final UUID id) {
        return entityManager.find(AocpAcceptedEmailStatus.class, id);
    }

    public AocpAcceptedEmailStatus save(final AocpAcceptedEmailStatus entity) {
        return entityManager.merge(entity);
    }

    public void remove(final AocpAcceptedEmailStatus entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM AocpAcceptedEmailStatus e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<AocpAcceptedEmailStatus> findAll() {
        return entityManager.createQuery("SELECT e FROM AocpAcceptedEmailStatus e", AocpAcceptedEmailStatus.class).getResultList();
    }
}
