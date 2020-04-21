package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.QueryResult;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.SingleResultType;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

/**
 * Repository for {@link CaseDetail}
 */
@SuppressWarnings("ALL")
@Repository
public abstract class CaseRepository extends AbstractEntityRepository<CaseDetail, UUID> implements CriteriaSupport<CaseDetail> {

    public void completeCase(final UUID caseId) {
        final CaseDetail caseDetail = findBy(caseId);

        if (caseDetail != null) {
            caseDetail.setCompleted(true);
            caseDetail.setAdjournedTo(null);
        }
    }

    @Query(value = "FROM CaseDetail cd WHERE UPPER(cd.urn) = UPPER(:urn)")
    public abstract CaseDetail findByUrn(@QueryParam("urn") String urn);

    @Query(value = "SELECT cd FROM CaseDetail cd " +
            "INNER JOIN cd.defendant dd " +
            "WHERE (UPPER(cd.urn) = UPPER(:urn) OR UPPER(REGEXP_REPLACE(cd.urn, '^[a-zA-Z]+', '')) = UPPER(:urn)) " +
            "AND UPPER(REPLACE(dd.personalDetails.address.postcode,' ','')) = UPPER(REPLACE(:postcode, ' ',''))", singleResult = SingleResultType.OPTIONAL)
    public abstract CaseDetail findByUrnPostcode(@QueryParam("urn") String urn,
                                                 @QueryParam("postcode") String postcode);

    @Query(value = "select cd from CaseDetail cd INNER JOIN cd.defendant dd WHERE dd.id = :defendantId")
    public abstract List<CaseDetail> findByDefendantId(@QueryParam("defendantId") final UUID defendantId);

    @Query(value = "SELECT cd FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cdocs IS NULL AND cd.completed IS NOT true AND cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter")
    public abstract QueryResult<CaseDetail> findCasesMissingSjpn(@QueryParam("prosecutingAuthorityFilter") String prosecutingAuthorityFilter);

    @Query(value = "SELECT cd FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cdocs IS NULL AND cd.postingDate < :postedBefore AND cd.completed IS NOT true AND cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter")
    public abstract QueryResult<CaseDetail> findCasesMissingSjpn(@QueryParam("prosecutingAuthorityFilter") String prosecutingAuthorityFilter, @QueryParam("postedBefore") final LocalDate postedBefore);

    @Query(value = "SELECT COUNT(cd) FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cdocs IS NULL AND cd.completed IS NOT true AND cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter")
    public abstract int countCasesMissingSjpn(@QueryParam("prosecutingAuthorityFilter") String prosecutingAuthorityFilter);

    @Query(value = "SELECT COUNT(cd) FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
            "WHERE cdocs IS NULL AND cd.postingDate < :postedBefore AND cd.completed IS NOT true AND cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter")
    public abstract int countCasesMissingSjpn(@QueryParam("prosecutingAuthorityFilter") String prosecutingAuthorityFilter, @QueryParam("postedBefore") final LocalDate postedBefore);

    @Query(value = "SELECT cd.caseDocuments FROM CaseDetail cd where cd.id = :caseId")
    public abstract List<CaseDocument> findCaseDocuments(@QueryParam("caseId") final UUID caseId);

    @Query(value = "SELECT cd.defendant FROM CaseDetail cd where cd.id = :caseId")
    public abstract DefendantDetail findCaseDefendant(@QueryParam("caseId") final UUID caseId);

    @Query(value = "select cd from CaseDetail cd JOIN cd.caseDocuments cdocs " +
            "WHERE cdocs.materialId = :materialId")
    public abstract CaseDetail findByMaterialId(@QueryParam("materialId") final UUID materialId);

    @Query(value = "SELECT cd.prosecutingAuthority FROM CaseDetail cd WHERE cd.id = :caseId", singleResult = SingleResultType.OPTIONAL)
    public abstract String getProsecutingAuthority(@QueryParam("caseId") final UUID caseId);

    @Query(value = "SELECT new uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence" +
            "(d.personalDetails.firstName, d.personalDetails.lastName, cd.id," +
            "d.personalDetails.address.address3, d.personalDetails.address.address4, d.personalDetails.address.address5," +
            "d.personalDetails.address.postcode, o.code, o.startDate, cd.prosecutingAuthority) " +
            "FROM CaseDetail cd " +
            "LEFT OUTER JOIN cd.defendant d " +
            "LEFT OUTER JOIN d.offences o " +
            "WHERE cd.id IN (SELECT rc.id FROM ReadyCase rc) " +
            "AND cd.id IN (SELECT cps.caseId FROM CasePublishStatus cps WHERE cps.numberOfPublishes < 5)" +
            "ORDER BY cd.postingDate")
    public abstract List<PendingCaseToPublishPerOffence> findPendingCasesToPublish();

    @Query(value = "SELECT DISTINCT new uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea" +
            "(e.id, e.urn, o.pleaDate, d.personalDetails.firstName, d.personalDetails.lastName, e.prosecutingAuthority, e.caseManagementStatus) " +
            "FROM CaseDetail e " +
            "JOIN e.defendant d " +
            "JOIN d.offences o " +
            "WHERE e.completed = false " +
            "AND o.plea = 'NOT_GUILTY' " +
            "AND e.caseStatus != 'REFER_FOR_COURT_HEARING' " +
            "AND e.prosecutingAuthority = :prosecutingAuthority " +
            "ORDER BY o.pleaDate DESC ")
    public abstract List<CaseNotGuiltyPlea> findCasesNotGuiltyPleaByProsecutingAuthority(@QueryParam("prosecutingAuthority") String prosecutingAuthority);

    @Query(value = "SELECT DISTINCT new uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea" +
            "(e.id, e.urn, o.pleaDate, d.personalDetails.firstName, d.personalDetails.lastName, e.prosecutingAuthority, e.caseManagementStatus) " +
            "FROM CaseDetail e " +
            "JOIN e.defendant d " +
            "JOIN d.offences o " +
            "WHERE e.completed = false " +
            "AND o.plea = 'NOT_GUILTY' " +
            "AND e.caseStatus != 'REFER_FOR_COURT_HEARING' " +
            "ORDER BY o.pleaDate DESC ")
    public abstract List<CaseNotGuiltyPlea> findCasesNotGuiltyPlea();

    public void updateDatesToAvoid(final UUID caseId, final String datesToAvoid) {
        findBy(caseId).setDatesToAvoid(datesToAvoid);
    }

}
