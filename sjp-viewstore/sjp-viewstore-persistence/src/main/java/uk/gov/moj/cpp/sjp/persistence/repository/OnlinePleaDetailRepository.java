package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class OnlinePleaDetailRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<OnlinePleaDetail> findByCaseIdAndDefendantId(final UUID caseId, final UUID defendantId) {
        return entityManager.createQuery(
                "SELECT e FROM OnlinePleaDetail e WHERE e.caseId = :caseId AND e.defendantId = :defendantId",
                OnlinePleaDetail.class)
                .setParameter("caseId", caseId)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }

    public List<OnlinePleaDetail> findByCaseIdAndDefendantIdAndAocpPleaIsNull(final UUID caseId, final UUID defendantId) {
        return entityManager.createQuery(
                "SELECT e FROM OnlinePleaDetail e WHERE e.caseId = :caseId AND e.defendantId = :defendantId AND e.aocpPlea IS NULL",
                OnlinePleaDetail.class)
                .setParameter("caseId", caseId)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }

    public List<OnlinePleaDetail> findByCaseIdAndDefendantIdAndAocpPlea(final UUID caseId, final UUID defendantId, final Boolean aocpPlea) {
        return entityManager.createQuery(
                "SELECT e FROM OnlinePleaDetail e WHERE e.caseId = :caseId AND e.defendantId = :defendantId AND e.aocpPlea = :aocpPlea",
                OnlinePleaDetail.class)
                .setParameter("caseId", caseId)
                .setParameter("defendantId", defendantId)
                .setParameter("aocpPlea", aocpPlea)
                .getResultList();
    }

    public OnlinePleaDetail findBy(final UUID id) {
        return entityManager.find(OnlinePleaDetail.class, id);
    }

    public OnlinePleaDetail save(final OnlinePleaDetail entity) {
        return entityManager.merge(entity);
    }

    public void remove(final OnlinePleaDetail entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM OnlinePleaDetail e", Long.class).getSingleResult();
    }

    public List<OnlinePleaDetail> findAll() {
        return entityManager.createQuery("SELECT e FROM OnlinePleaDetail e", OnlinePleaDetail.class).getResultList();
    }
}
