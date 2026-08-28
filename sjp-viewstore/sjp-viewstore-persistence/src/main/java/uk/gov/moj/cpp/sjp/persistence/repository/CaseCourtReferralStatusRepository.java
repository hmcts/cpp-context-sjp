package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseCourtReferralStatusRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public CaseCourtReferralStatus findBy(final UUID id) {
        return entityManager.find(CaseCourtReferralStatus.class, id);
    }

    public CaseCourtReferralStatus save(final CaseCourtReferralStatus entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseCourtReferralStatus entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseCourtReferralStatus e", Long.class).getSingleResult();
    }

    public List<CaseCourtReferralStatus> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseCourtReferralStatus e", CaseCourtReferralStatus.class).getResultList();
    }
}
