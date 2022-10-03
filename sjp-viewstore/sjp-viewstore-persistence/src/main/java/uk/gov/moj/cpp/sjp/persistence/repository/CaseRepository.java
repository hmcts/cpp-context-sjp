package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseWithoutDefendantPostcode;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
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
@SuppressWarnings({"ALL", "PMD.BeanMembersShouldSerialize"})
@Repository
public abstract class CaseRepository extends AbstractEntityRepository<CaseDetail, UUID> implements CriteriaSupport<CaseDetail> {

    @Inject
    private EntityManager entityManager;


    private static final String SELECT_CASES_FOR_SOC_CHECK =
            " with adjourn_temp as (select distinct cd.id from case_decision cd inner join offence_decision od on cd.id = od.case_decision_id where od.decision_type = 'ADJOURN')" +
                    " select distinct cast(cd.id as varchar) as id , cd.urn as urn, cdn.saved_at as lastUpdatedDate, cd.prosecuting_authority as prosecutingAuthority, sess.magistrate as magistrate, cast(sess.user_id as varchar) as legalAdvisorUserId" +
                    " from case_details cd inner join case_decision cdn on cd.id = cdn.case_id " +
                    " inner join offence_decision od on cdn.id = od.case_decision_id " +
                    " inner join session sess on sess.id = cdn.session_id " +
                    " where cdn.saved_at >= :fromDate and cdn.saved_at  <= :toDate " +
                    " and cdn.id not in (select atemp.id from adjourn_temp atemp)" +
                    " and cd.id not in (select soc.case_id from soc_check soc)" +
                    " and cd.completed = true" +
                    " and sess.user_id != :loggedInUserId" +
                    " and sess.local_justice_area_national_court_code = :ljaCode and sess.court_house_code = :courtHouseCode" +
                    " order by";

    @SuppressWarnings("unchecked")
    public List<Object[]> findCasesForSOCCheck(final String loggedInUserId,
                                               final String ljaCode,
                                               final String courtHouseCode,
                                               final LocalDate fromDate,
                                               final LocalDate toDate,
                                               final String sortField,
                                               final String sortOrder) {

        return entityManager.createNativeQuery(getOrderingString(sortField, sortOrder))
                .setParameter("loggedInUserId", UUID.fromString(loggedInUserId))
                .setParameter("ljaCode", ljaCode)
                .setParameter("courtHouseCode", courtHouseCode)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private String getOrderingString(final String sortField, final String sortOrder) {
        final StringBuilder sb = new StringBuilder(SELECT_CASES_FOR_SOC_CHECK)
                .append(StringUtils.leftPad(sortField, sortField.length() + 1))
                .append(StringUtils.leftPad(sortOrder, sortOrder.length() + 1));
        return sb.toString();
    }


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
            "AND UPPER(REPLACE(dd.address.postcode,' ','')) = UPPER(REPLACE(:postcode, ' ',''))", singleResult = SingleResultType.OPTIONAL)
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
            "(d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, d.personalDetails.dateOfBirth," +
            "cd.id, cd.urn," +
            "d.address.address1, d.address.address2," +
            "d.address.address3, d.address.address4, d.address.address5," +
            "d.address.postcode, o.code, o.startDate, o.wording, " +
            "o.pressRestriction.requested, o.pressRestriction.name, o.completed, cd.prosecutingAuthority) " +
            "FROM CaseDetail cd " +
            "LEFT OUTER JOIN cd.defendant d " +
            "LEFT OUTER JOIN d.offences o " +
            "WHERE cd.id IN (SELECT rc.id FROM ReadyCase rc) " +
            "AND cd.id IN (SELECT cps.caseId FROM CasePublishStatus cps WHERE cps.numberOfPublishes < 5)" +
            "ORDER BY cd.postingDate")
    public abstract List<PendingCaseToPublishPerOffence> findPublicTransparencyReportPendingCases();

    @Query(value = "SELECT new uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence" +
            "(d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, d.personalDetails.dateOfBirth," +
            "cd.id, cd.urn," +
            "d.address.address1, d.address.address2," +
            "d.address.address3, d.address.address4, d.address.address5," +
            "d.address.postcode, o.code, o.startDate, o.wording," +
            "o.pressRestriction.requested, o.pressRestriction.name, o.completed, cd.prosecutingAuthority) " +
            "FROM CaseDetail cd " +
            "LEFT OUTER JOIN cd.defendant d " +
            "LEFT OUTER JOIN d.offences o " +
            "WHERE cd.id IN (SELECT rc.id FROM ReadyCase rc) " +
            "ORDER BY cd.postingDate")
    public abstract List<PendingCaseToPublishPerOffence> findPressTransparencyReportPendingCases();

    @Query(value = "SELECT DISTINCT new uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea" +
            "(e.id, e.urn, o.pleaDate, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, e.prosecutingAuthority, e.caseManagementStatus) " +
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
            "(e.id, e.urn, o.pleaDate, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, e.prosecutingAuthority, e.caseManagementStatus) " +
            "FROM CaseDetail e " +
            "JOIN e.defendant d " +
            "JOIN d.offences o " +
            "WHERE e.completed = false " +
            "AND o.plea = 'NOT_GUILTY' " +
            "AND e.caseStatus != 'REFER_FOR_COURT_HEARING' " +
            "ORDER BY o.pleaDate DESC ")
    public abstract List<CaseNotGuiltyPlea> findCasesNotGuiltyPlea();

    @Query(value = "SELECT DISTINCT new uk.gov.moj.cpp.sjp.persistence.entity.CaseWithoutDefendantPostcode" +
            "(e.id, e.urn, e.postingDate, d.personalDetails.firstName, d.personalDetails.lastName, e.prosecutingAuthority,e.legalEntityDetails.legalEntityName) " +
            "FROM CaseDetail e " +
            "JOIN e.defendant d " +
            "WHERE e.completed = false " +
            "AND d.personalDetails.address.postcode IS NULL " +
            "ORDER BY e.postingDate DESC ")
    public abstract List<CaseWithoutDefendantPostcode> findCasesWithoutDefendantPostcode();

    public void updateDatesToAvoid(final UUID caseId, final String datesToAvoid) {
        findBy(caseId).setDatesToAvoid(datesToAvoid);
    }

}
