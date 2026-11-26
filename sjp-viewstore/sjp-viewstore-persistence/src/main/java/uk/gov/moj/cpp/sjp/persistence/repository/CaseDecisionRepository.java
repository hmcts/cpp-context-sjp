package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CaseDecisionRepository extends JPAEntityRepository<CaseDecision, UUID> {

    @Override
    protected Class<CaseDecision> getEntityClass() {
        return CaseDecision.class;
    }

    public List<CaseDecision> findCaseDecisionsForConvictingCourtSessions(final UUID offenceId) {
        return entityManager.createQuery("SELECT casedecision FROM CaseDecision as casedecision INNER JOIN casedecision.offenceDecisions as offencedecision " +
                        "WHERE offencedecision.offenceId = :offenceId and offencedecision.caseDecisionId = casedecision.id and offencedecision.verdictType IN ('FOUND_GUILTY', 'PROVED_SJP')", CaseDecision.class)
                .setParameter("offenceId", offenceId)
                .getResultList();
    }

    public CaseDecision findCaseDecisionById(final UUID caseId) {
        return entityManager.createQuery("SELECT casedecision_a FROM CaseDecision as casedecision_a  " +
                        "WHERE casedecision_a.caseId = :caseId  and casedecision_a.savedAt=(SELECT MAX(casedecision_b.savedAt) FROM CaseDecision as casedecision_b  " +
                        "WHERE casedecision_b.caseId = casedecision_a.caseId )", CaseDecision.class)
                .setParameter("caseId", caseId)
                .getResultStream().findFirst().orElse(null);
    }
}