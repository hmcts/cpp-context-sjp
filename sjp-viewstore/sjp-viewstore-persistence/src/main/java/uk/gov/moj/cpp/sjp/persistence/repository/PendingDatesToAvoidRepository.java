package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link PendingDatesToAvoid}
 */
@Repository
public abstract class PendingDatesToAvoidRepository implements EntityRepository<PendingDatesToAvoid, UUID> {

    @Query(value = "SELECT pda FROM PendingDatesToAvoid as pda INNER JOIN pda.caseDetail as cd " +
            "WHERE cd.datesToAvoid IS NULL AND cd.assigneeId IS NULL AND cd.completed = false " +
            "AND cd.prosecutingAuthority like :prosecutingAuthority " +
            "ORDER BY pda.pleaDate ASC")
    public abstract List<PendingDatesToAvoid> findCasesPendingDatesToAvoid(@QueryParam("prosecutingAuthority") String prosecutingAuthority);

    public List<PendingDatesToAvoid> findCasesPendingDatesToAvoid(ProsecutingAuthority prosecutingAuthority) {
        return findCasesPendingDatesToAvoid(prosecutingAuthority == null ? null : prosecutingAuthority.name());
    }

    public void removeByCaseId(final UUID caseId) {
        Optional.ofNullable(this.findBy(caseId)).ifPresent(this::remove);
    }
}
