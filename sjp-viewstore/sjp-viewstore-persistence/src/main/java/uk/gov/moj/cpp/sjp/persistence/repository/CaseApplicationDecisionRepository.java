package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseApplicationDecisionRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public CaseApplicationDecision findBy(final UUID id) {
        return entityManager.find(CaseApplicationDecision.class, id);
    }

    public CaseApplicationDecision save(final CaseApplicationDecision entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseApplicationDecision entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseApplicationDecision e", Long.class).getSingleResult();
    }

    public List<CaseApplicationDecision> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseApplicationDecision e", CaseApplicationDecision.class).getResultList();
    }
}
