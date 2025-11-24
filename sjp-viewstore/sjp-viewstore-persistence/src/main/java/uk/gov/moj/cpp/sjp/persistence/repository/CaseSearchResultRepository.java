package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseSearchResultRepository extends EntityRepository<CaseSearchResult, UUID> {

    @Query("from CaseSearchResult as r inner join fetch r.caseSummary as c where upper(r.lastName) = upper(:lastName) and r.dateAdded = " +
            "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId and upper(z.lastName) = upper(:lastName)) " +
            "and r.caseSummary.prosecutingAuthority like :prosecutingAuthority " +
            "order by r.firstName ASC, c.postingDate DESC")
    List<CaseSearchResult> findByLastName(@QueryParam("prosecutingAuthority") String prosecutingAuthority, @QueryParam("lastName") String lastName);

    @Query("from CaseSearchResult as r inner join fetch r.caseSummary as c where upper(r.caseSummary.urn) = upper(:urn) and r.dateAdded = " +
            "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId) " +
            "and r.caseSummary.prosecutingAuthority like :prosecutingAuthority " +
            "order by r.firstName ASC, c.postingDate DESC")
    List<CaseSearchResult> findByUrn(@QueryParam("prosecutingAuthority") String prosecutingAuthority, @QueryParam("urn") String urn);

    List<CaseSearchResult> findByCaseId(UUID caseId);

    @Query("from CaseSearchResult as r inner join fetch r.caseSummary as c where upper(r.legalEntityName) = upper(:legalEntityName) and r.dateAdded = " +
            "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId and upper(z.legalEntityName) = upper(:legalEntityName)) " +
            "and r.caseSummary.prosecutingAuthority like :prosecutingAuthority " +
            "order by r.legalEntityName ASC, c.postingDate DESC")
    List<CaseSearchResult> findByLegalEntityName(@QueryParam("prosecutingAuthority") String prosecutingAuthority, @QueryParam("legalEntityName") String legalEntityName);
}
