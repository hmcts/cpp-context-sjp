package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseApplicationRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public CaseDetail findByApplicationDecisionId(final UUID applicationDecisionId) {
        return entityManager.createQuery("SELECT ca.caseDetail FROM CaseApplication ca WHERE ca.applicationDecision.decisionId=:applicationDecisionId", CaseDetail.class)
                .setParameter("applicationDecisionId", applicationDecisionId)
                .getSingleResult();
    }

    public CaseDetail findByApplicationId(final UUID applicationId) {
        return entityManager.createQuery("SELECT ca.caseDetail FROM CaseApplication ca WHERE ca.id=:applicationId", CaseDetail.class)
                .setParameter("applicationId", applicationId)
                .getSingleResult();
    }

    public CaseApplication findBy(final UUID id) {
        return entityManager.find(CaseApplication.class, id);
    }

    public CaseApplication save(final CaseApplication entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseApplication entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseApplication e", Long.class).getSingleResult();
    }

    public List<CaseApplication> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseApplication e", CaseApplication.class).getResultList();
    }
}
