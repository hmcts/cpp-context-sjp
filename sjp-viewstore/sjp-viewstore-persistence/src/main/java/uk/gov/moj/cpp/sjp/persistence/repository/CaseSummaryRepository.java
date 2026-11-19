package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CaseSummaryRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public CaseSummary findBy(final UUID id) {
        return entityManager.find(CaseSummary.class, id);
    }

    public CaseSummary save(final CaseSummary entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseSummary entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseSummary e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<CaseSummary> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseSummary e", CaseSummary.class).getResultList();
    }
}