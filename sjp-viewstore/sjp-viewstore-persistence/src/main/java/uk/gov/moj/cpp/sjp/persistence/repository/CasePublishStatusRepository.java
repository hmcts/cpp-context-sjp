package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CasePublishStatusRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<CasePublishStatus> findByCaseIds(final List<UUID> caseIds) {
        return entityManager.createQuery(
                "SELECT cps FROM CasePublishStatus cps WHERE cps.caseId in :caseIds ORDER BY cps.firstPublished DESC",
                CasePublishStatus.class)
                .setParameter("caseIds", caseIds)
                .getResultList();
    }

    public CasePublishStatus findBy(final UUID id) {
        return entityManager.find(CasePublishStatus.class, id);
    }

    public CasePublishStatus save(final CasePublishStatus entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CasePublishStatus entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CasePublishStatus e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<CasePublishStatus> findAll() {
        return entityManager.createQuery("SELECT e FROM CasePublishStatus e", CasePublishStatus.class).getResultList();
    }
}
