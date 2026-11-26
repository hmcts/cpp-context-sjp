package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReadyCaseRepository extends JPAEntityRepository<ReadyCase, UUID> {

    @Override
    protected Class<ReadyCase> getEntityClass() {
        return ReadyCase.class;
    }

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
}
