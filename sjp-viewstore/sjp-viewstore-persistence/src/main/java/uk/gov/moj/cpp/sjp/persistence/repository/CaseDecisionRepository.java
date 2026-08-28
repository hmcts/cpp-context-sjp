package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseDecisionRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

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
                .getSingleResult();
    }

    public CaseDecision findBy(final UUID id) {
        return entityManager.find(CaseDecision.class, id);
    }

    public CaseDecision save(final CaseDecision entity) {
        // Mirror the DeltaSpike EntityRepository.save contract: persist a genuinely-new entity
        // (leaving the passed instance managed, and its associations unresolved) else merge.
        if (entity.getId() != null && entityManager.find(CaseDecision.class, entity.getId()) != null) {
            return entityManager.merge(entity);
        }
        entityManager.persist(entity);
        return entity;
    }

    public void remove(final CaseDecision entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseDecision e", Long.class).getSingleResult();
    }

    public List<CaseDecision> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseDecision e", CaseDecision.class).getResultList();
    }
}
