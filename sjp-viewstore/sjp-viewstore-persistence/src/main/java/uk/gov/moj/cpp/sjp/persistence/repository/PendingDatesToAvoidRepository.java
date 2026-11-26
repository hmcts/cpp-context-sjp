package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PendingDatesToAvoid}
 */
@ApplicationScoped
public class PendingDatesToAvoidRepository extends JPAEntityRepository<PendingDatesToAvoid, UUID> {

    @Override
    protected Class<PendingDatesToAvoid> getEntityClass() {
        return PendingDatesToAvoid.class;
    }

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
}
