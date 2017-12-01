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

    @Query("from CaseSearchResult as r inner join fetch r.caseSummary where upper(r.lastName) = upper(:lastName) order by r.firstName")
    List<CaseSearchResult> findByLastName(@QueryParam("lastName") String lastName);

    @Query("from CaseSearchResult as r inner join fetch r.caseSummary where upper(r.caseSummary.urn) = upper(:urn)")
    List<CaseSearchResult> findByCaseSummary_urn(@QueryParam("urn") String urn);

    List<CaseSearchResult> findByCaseIdAndPersonId(UUID caseId, UUID personId);

    List<CaseSearchResult> findByPersonId(UUID personId);

    List<CaseSearchResult> findByCaseId(UUID caseId);
}
