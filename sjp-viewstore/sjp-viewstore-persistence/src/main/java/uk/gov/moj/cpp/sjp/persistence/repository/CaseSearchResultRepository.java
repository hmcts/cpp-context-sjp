package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseSearchResultRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<CaseSearchResult> findByLastName(final String prosecutingAuthority, final String lastName,
                                                 final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery(
                "from CaseSearchResult as r inner join fetch r.caseSummary as c where upper(r.lastName) = upper(:lastName) and r.dateAdded = " +
                        "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId and upper(z.lastName) = upper(:lastName)) " +
                        "and (r.caseSummary.prosecutingAuthority like :prosecutingAuthority OR r.caseSummary.prosecutingAuthority IN (:agentProsecutorAuthorityAccess)) " +
                        "order by r.firstName ASC, c.postingDate DESC",
                CaseSearchResult.class)
                .setParameter("prosecutingAuthority", prosecutingAuthority)
                .setParameter("lastName", lastName)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getResultList();
    }

    public List<CaseSearchResult> findByUrn(final String prosecutingAuthority, final String urn,
                                            final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery(
                "from CaseSearchResult as r inner join fetch r.caseSummary as c where upper(r.caseSummary.urn) = upper(:urn) and r.dateAdded = " +
                        "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId) " +
                        "and (r.caseSummary.prosecutingAuthority like :prosecutingAuthority OR r.caseSummary.prosecutingAuthority IN (:agentProsecutorAuthorityAccess)) " +
                        "order by r.firstName ASC, c.postingDate DESC",
                CaseSearchResult.class)
                .setParameter("prosecutingAuthority", prosecutingAuthority)
                .setParameter("urn", urn)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getResultList();
    }

    public List<CaseSearchResult> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                "SELECT e FROM CaseSearchResult e WHERE e.caseId = :caseId",
                CaseSearchResult.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public List<CaseSearchResult> findByLegalEntityName(final String prosecutingAuthority, final String legalEntityName,
                                                        final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery(
                "from CaseSearchResult as r inner join fetch r.caseSummary as c where upper(r.legalEntityName) = upper(:legalEntityName) and r.dateAdded = " +
                        "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId and upper(z.legalEntityName) = upper(:legalEntityName)) " +
                        "and (r.caseSummary.prosecutingAuthority like :prosecutingAuthority OR r.caseSummary.prosecutingAuthority IN (:agentProsecutorAuthorityAccess)) " +
                        "order by r.legalEntityName ASC, c.postingDate DESC",
                CaseSearchResult.class)
                .setParameter("prosecutingAuthority", prosecutingAuthority)
                .setParameter("legalEntityName", legalEntityName)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getResultList();
    }

    public CaseSearchResult findBy(final UUID id) {
        return entityManager.find(CaseSearchResult.class, id);
    }

    public CaseSearchResult save(final CaseSearchResult entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseSearchResult entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseSearchResult e", Long.class).getSingleResult();
    }

    public List<CaseSearchResult> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseSearchResult e", CaseSearchResult.class).getResultList();
    }
}
