package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseDecisionRepository extends EntityRepository<CaseDecision, UUID> {

    @Query(value = "SELECT casedecision FROM CaseDecision as casedecision INNER JOIN casedecision.offenceDecisions as offencedecision " +
    "WHERE offencedecision.offenceId = :offenceId and offencedecision.caseDecisionId = casedecision.id and offencedecision.verdictType IN ('FOUND_GUILTY', 'PROVED_SJP')")
    List<CaseDecision> findCaseDecisionsForConvictingCourtSessions(@QueryParam("offenceId") final UUID offenceId);

    @Query(value = "SELECT casedecision FROM CaseDecision as casedecision WHERE casedecision.caseId = :caseId")
    CaseDecision findCaseDecisionById(@QueryParam("caseId") final UUID caseId);
}
