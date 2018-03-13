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

    @Query("from CaseSearchResult as r inner join fetch r.caseSummary as c where cast(r.id as string) IN " +
            "(select cast(x.id as string) from CaseSearchResult as x where upper(x.lastName) = upper(:lastName) and x.caseId = r.caseId and x.dateAdded = " +
            "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=x.caseId and upper(z.lastName) = upper(:lastName))" +
            ") order by r.firstName ASC, c.postingDate DESC")
    List<CaseSearchResult> findByLastName(@QueryParam("lastName") String lastName);

    @Query("from CaseSearchResult as r inner join fetch r.caseSummary as c where upper(r.caseSummary.urn) = upper(:urn) and r.dateAdded = " +
            "(select max(z.dateAdded) from CaseSearchResult as z where z.caseId=r.caseId) order by r.firstName ASC, c.postingDate DESC")
    List<CaseSearchResult> findByCaseSummary_urn(@QueryParam("urn") String urn);

    List<CaseSearchResult> findByCaseId(UUID caseId);
}
