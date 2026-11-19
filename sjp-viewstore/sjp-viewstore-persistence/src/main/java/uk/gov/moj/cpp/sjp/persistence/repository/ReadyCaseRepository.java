package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReadyCaseRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<ReadyCase> findByAssigneeId(final UUID assigneeId) {
        return entityManager.createQuery(
                "SELECT e FROM ReadyCase e WHERE e.assigneeId = :assigneeId",
                ReadyCase.class)
                .setParameter("assigneeId", assigneeId)
                .getResultList();
    }

    public ReadyCase findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                "SELECT e FROM ReadyCase e WHERE e.caseId = :caseId",
                ReadyCase.class)
                .setParameter("caseId", caseId)
                .getResultStream().findFirst().orElse(null);
    }

    public ReadyCase findBy(final UUID id) {
        return entityManager.find(ReadyCase.class, id);
    }

    public ReadyCase save(final ReadyCase entity) {
        return entityManager.merge(entity);
    }

    public void remove(final ReadyCase entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM ReadyCase e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<ReadyCase> findAll() {
        return entityManager.createQuery("SELECT e FROM ReadyCase e", ReadyCase.class).getResultList();
    }
}
