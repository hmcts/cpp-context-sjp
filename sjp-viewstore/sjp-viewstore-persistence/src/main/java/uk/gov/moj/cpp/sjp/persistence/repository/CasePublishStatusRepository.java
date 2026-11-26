package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CasePublishStatusRepository extends JPAEntityRepository<CasePublishStatus, UUID> {

    @Override
    protected Class<CasePublishStatus> getEntityClass() {
        return CasePublishStatus.class;
    }

    public List<CasePublishStatus> findByCaseIds(final List<UUID> caseIds) {
        return entityManager.createQuery(
                "SELECT cps FROM CasePublishStatus cps WHERE cps.caseId in :caseIds ORDER BY cps.firstPublished DESC",
                CasePublishStatus.class)
                .setParameter("caseIds", caseIds)
                .getResultList();
    }
}
