package uk.gov.moj.cpp.sjp.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.util.UUID;

@Repository
public interface CaseApplicationRepository extends EntityRepository<CaseApplication, UUID> {

    @Query("SELECT ca.caseDetail FROM CaseApplication ca WHERE ca.applicationDecision.decisionId=:applicationDecisionId")
    CaseDetail findByApplicationDecisionId(@QueryParam("applicationDecisionId") UUID applicationDecisionId);

    @Query("SELECT ca.caseDetail FROM CaseApplication ca WHERE ca.id=:applicationId")
    CaseDetail findByApplicationId(@QueryParam("applicationId") UUID applicationId);

}


