package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository for {@link PendingDatesToAvoid}
 */
@ApplicationScoped
public class PendingDatesToAvoidRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<PendingDatesToAvoid> findCasesPendingDatesToAvoid(String prosecutingAuthority,
                                                                  List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery(
                "SELECT pda FROM PendingDatesToAvoid as pda INNER JOIN pda.caseDetail as cd " +
                        "WHERE cd.datesToAvoid IS NULL AND cd.assigneeId IS NULL AND cd.completed = false " +
                        "AND (cd.prosecutingAuthority like :prosecutingAuthority OR cd.prosecutingAuthority IN (:agentProsecutorAuthorityAccess) ) " +
                        "ORDER BY pda.pleaDate ASC",
                PendingDatesToAvoid.class)
                .setParameter("prosecutingAuthority", prosecutingAuthority)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getResultList();
    }

    public void removeByCaseId(final UUID caseId) {
        Optional.ofNullable(this.findBy(caseId)).ifPresent(this::remove);
    }

    public PendingDatesToAvoid findBy(final UUID id) {
        return entityManager.find(PendingDatesToAvoid.class, id);
    }

    public PendingDatesToAvoid save(final PendingDatesToAvoid entity) {
        return entityManager.merge(entity);
    }

    public void remove(final PendingDatesToAvoid entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM PendingDatesToAvoid e", Long.class).getSingleResult();
    }

    public List<PendingDatesToAvoid> findAll() {
        return entityManager.createQuery("SELECT e FROM PendingDatesToAvoid e", PendingDatesToAvoid.class).getResultList();
    }
}
