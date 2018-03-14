package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link PendingDatesToAvoid}
 */
@Repository
public interface PendingDatesToAvoidRepository extends EntityRepository<PendingDatesToAvoid, UUID> {

    @Query(value = "SELECT pda FROM PendingDatesToAvoid as pda INNER JOIN pda.caseDetail as cd " +
            "WHERE cd.datesToAvoid IS NULL AND cd.assigneeId IS NULL AND cd.completed = false " +
            "ORDER BY pda.pleaDate ASC")
    List<PendingDatesToAvoid> findCasesPendingDatesToAvoid();

    @Query(value = "DELETE FROM PendingDatesToAvoid WHERE caseId=:caseId")
    void removeByCaseId(@QueryParam("caseId") UUID caseId);
}
