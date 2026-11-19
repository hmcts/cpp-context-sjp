package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FinancialMeansRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public FinancialMeans findBy(final UUID id) {
        return entityManager.find(FinancialMeans.class, id);
    }

    public FinancialMeans save(final FinancialMeans entity) {
        return entityManager.merge(entity);
    }

    public void remove(final FinancialMeans entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM FinancialMeans e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<FinancialMeans> findAll() {
        return entityManager.createQuery("SELECT e FROM FinancialMeans e", FinancialMeans.class).getResultList();
    }
}
