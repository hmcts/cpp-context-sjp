package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CasePublishStatusRepository extends EntityRepository<CasePublishStatus, UUID> {

    @Query(value = "SELECT cps FROM CasePublishStatus cps WHERE cps.caseId in :caseIds ORDER BY cps.firstPublished DESC")
    List<CasePublishStatus> findByCaseIds(@QueryParam("caseIds") final List<UUID> caseIds);

}
