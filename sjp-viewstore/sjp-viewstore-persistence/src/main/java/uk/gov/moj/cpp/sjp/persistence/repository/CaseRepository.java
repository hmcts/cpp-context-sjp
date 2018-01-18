package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetailMissingSjpn;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.MaxResults;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.QueryResult;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.SingleResultType;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

/**
 * Repository for {@link CaseDetail}
 */
@SuppressWarnings("WeakerAccess")
@Repository
public abstract class CaseRepository extends AbstractEntityRepository<CaseDetail, UUID> implements CriteriaSupport<CaseDetail> {

    public void completeCase(final UUID caseId) {
        final CaseDetail caseDetail = findBy(caseId);

        if (caseDetail != null) {
            caseDetail.setCompleted(true);
        }
    }

    public void requestWithdrawalAllOffences(final UUID caseId) {
        final CaseDetail caseDetail = findBy(caseId);

        if (caseDetail != null) {
            caseDetail.getDefendants().forEach(defendant -> defendant.getOffences()
                    .forEach(offence -> offence.setPendingWithdrawal(true)));
        }
    }

    public void cancelRequestWithdrawalAllOffences(final UUID caseId) {
        final CaseDetail caseDetail = findBy(caseId);

        if (caseDetail != null) {
            caseDetail.getDefendants().forEach(defendant -> defendant.getOffences()
                    .forEach(offence -> offence.setPendingWithdrawal(false)));
        }
    }

    @Query(value = "FROM CaseDetail cd WHERE UPPER(cd.urn) = UPPER(:urn)")
    public abstract CaseDetail findByUrn(@QueryParam("urn") String urn);

    @Query(value = "FROM CaseDetail cd WHERE UPPER(cd.urn) = UPPER(:urn) and cd.initiationCode = 'J'", singleResult = SingleResultType.OPTIONAL)
    public abstract CaseDetail findSjpCaseByUrn(@QueryParam("urn") String urn);

    @Query(value = "select cd from CaseDetail cd INNER JOIN cd.defendants dd WHERE dd.personId = :personId")
    public abstract List<CaseDetail> findByPersonId(@QueryParam("personId") final UUID personId);

    @Query(value = "SELECT cd FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cd.initiationCode = 'J' AND cdocs IS NULL AND cd.completed IS NOT true")
    public abstract QueryResult<CaseDetail> findCasesMissingSjpn();

    @Query(value = "SELECT cd FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cd.initiationCode = 'J' AND cdocs IS NULL AND cd.postingDate < :postedBefore AND cd.completed IS NOT true")
    public abstract QueryResult<CaseDetail> findCasesMissingSjpn(@QueryParam("postedBefore") final LocalDate postedBefore);

    @Query(value = "select new uk.gov.moj.cpp.sjp.persistence.entity.CaseDetailMissingSjpn" +
            "(cd.id, cd.urn, cd.postingDate, res.firstName, res.lastName) " +
            "from CaseDetail cd " +
            "LEFT OUTER JOIN cd.caseDocuments cdocs " +
            "ON cdocs.documentType = 'SJPN' " +
            "LEFT OUTER JOIN cd.caseSearchResults res " +
            "WHERE cd.initiationCode = 'J' " +
            "AND cdocs IS NULL " +
            "AND cd.completed IS NOT true")
    public abstract List<CaseDetailMissingSjpn> findCasesMissingSjpnWithDetails();

    @Query(value = "select new uk.gov.moj.cpp.sjp.persistence.entity.CaseDetailMissingSjpn" +
            "(cd.id, cd.urn, cd.postingDate, res.firstName, res.lastName) " +
            "from CaseDetail cd " +
            "LEFT OUTER JOIN cd.caseDocuments cdocs " +
            "ON cdocs.documentType = 'SJPN' " +
            "LEFT OUTER JOIN cd.caseSearchResults res " +
            "WHERE cd.initiationCode = 'J' " +
            "AND cdocs IS NULL " +
            "AND cd.postingDate < :postedBefore " +
            "AND cd.completed IS NOT true")
    public abstract List<CaseDetailMissingSjpn> findCasesMissingSjpnWithDetails(@QueryParam("postedBefore") final LocalDate postedBefore);

    @Query(value = "SELECT COUNT(cd) FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cd.initiationCode = 'J' AND cdocs IS NULL AND cd.completed IS NOT true")
    public abstract int countCasesMissingSjpn();

    @Query(value = "SELECT COUNT(cd) FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cd.initiationCode = 'J' AND cdocs IS NULL AND cd.postingDate < :postedBefore AND cd.completed IS NOT true")
    public abstract int countCasesMissingSjpn(@QueryParam("postedBefore") final LocalDate postedBefore);

    @Query(value = "SELECT cd.caseDocuments FROM CaseDetail cd where cd.id = :caseId")
    public abstract List<CaseDocument> findCaseDocuments(@QueryParam("caseId") final UUID caseId);

    @Query(value = "SELECT cd.defendants FROM CaseDetail cd where cd.id = :caseId")
    public abstract List<DefendantDetail> findCaseDefendants(@QueryParam("caseId") final UUID caseId);

    @Query(value = "select cd from CaseDetail cd JOIN cd.caseDocuments cdocs " +
            "WHERE cdocs.materialId = :materialId" )
    public abstract CaseDetail findByMaterialId(@QueryParam("materialId") final UUID materialId);

    @Query(value = "SELECT DISTINCT cd FROM CaseDetail cd JOIN cd.caseDocuments doc ON doc.documentType = 'SJPN' " +
            "WHERE cd.initiationCode = 'J' AND cd.completed IS NOT true ORDER BY cd.postingDate")
    public abstract List<CaseDetail> findAwaitingSjpCases(@MaxResults final int limit);

    @Query(value = "SELECT min(cd.postingDate) FROM CaseDetail cd " +
            "WHERE cd.initiationCode = 'J' AND cd.completed IS NOT true")
    public abstract LocalDate findOldestUncompletedPostingDate();

}
