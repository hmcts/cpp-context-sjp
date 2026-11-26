package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OnlinePleaDetailRepository extends JPAEntityRepository<OnlinePleaDetail, UUID> {

    @Override
    protected Class<OnlinePleaDetail> getEntityClass() {
        return OnlinePleaDetail.class;
    }

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
}
