package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DefendantDetailUpdateRequestRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public DefendantDetailUpdateRequest findBy(final UUID id) {
        return entityManager.find(DefendantDetailUpdateRequest.class, id);
    }

    public DefendantDetailUpdateRequest save(final DefendantDetailUpdateRequest entity) {
        return entityManager.merge(entity);
    }

    public void remove(final DefendantDetailUpdateRequest entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM DefendantDetailUpdateRequest e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<DefendantDetailUpdateRequest> findAll() {
        return entityManager.createQuery("SELECT e FROM DefendantDetailUpdateRequest e", DefendantDetailUpdateRequest.class).getResultList();
    }
}
