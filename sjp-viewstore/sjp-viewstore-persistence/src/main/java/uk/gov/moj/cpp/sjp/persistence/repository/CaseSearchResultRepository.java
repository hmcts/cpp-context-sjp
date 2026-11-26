package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CaseSearchResultRepository extends JPAEntityRepository<CaseSearchResult, UUID> {

    @Override
    protected Class<CaseSearchResult> getEntityClass() {
        return CaseSearchResult.class;
    }

    public List<CaseSearchResult> findByLastName(final String prosecutingAuthority, final String lastName,
                                                 final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery(
                        "select r from CaseSearchResult as r inner join fetch r.caseSummary where upper(r.lastName) = upper(:lastName) and r.dateAdded = " +
                                "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId and upper(z.lastName) = upper(:lastName)) " +
                                "and (r.caseSummary.prosecutingAuthority like :prosecutingAuthority OR r.caseSummary.prosecutingAuthority IN (:agentProsecutorAuthorityAccess)) " +
                                "order by r.firstName ASC, r.caseSummary.postingDate DESC",
                        CaseSearchResult.class)
                .setParameter("prosecutingAuthority", prosecutingAuthority)
                .setParameter("lastName", lastName)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getResultList();
    }

    public List<CaseSearchResult> findByUrn(final String prosecutingAuthority, final String urn,
                                            final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery(
                        "select r from CaseSearchResult as r inner join fetch r.caseSummary where upper(r.caseSummary.urn) = upper(:urn) and r.dateAdded = " +
                                "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId) " +
                                "and (r.caseSummary.prosecutingAuthority like :prosecutingAuthority OR r.caseSummary.prosecutingAuthority IN (:agentProsecutorAuthorityAccess)) " +
                                "order by r.firstName ASC, r.caseSummary.postingDate DESC",
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
                        "select r from CaseSearchResult as r inner join fetch r.caseSummary where upper(r.legalEntityName) = upper(:legalEntityName) and r.dateAdded = " +
                                "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId and upper(z.legalEntityName) = upper(:legalEntityName)) " +
                                "and (r.caseSummary.prosecutingAuthority like :prosecutingAuthority OR r.caseSummary.prosecutingAuthority IN (:agentProsecutorAuthorityAccess)) " +
                                "order by r.legalEntityName ASC, r.caseSummary.postingDate DESC",
                        CaseSearchResult.class)
                .setParameter("prosecutingAuthority", prosecutingAuthority)
                .setParameter("legalEntityName", legalEntityName)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getResultList();
    }

    public CaseSearchResult saveAndFlush(final CaseSearchResult entity) {
        final CaseSearchResult merged = entityManager.merge(entity);
        entityManager.flush();
        return merged;
    }
}