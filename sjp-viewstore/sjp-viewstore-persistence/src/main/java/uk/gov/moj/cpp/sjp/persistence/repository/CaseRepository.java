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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

/**
 * Repository for {@link CaseDetail}
 */
@SuppressWarnings({"ALL", "PMD.BeanMembersShouldSerialize"})
@ApplicationScoped
public class CaseRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    private static final String SELECT_CASES_FOR_SOC_CHECK =
            " with adjourn_temp as (select distinct cd.id from case_decision cd inner join offence_decision od on cd.id = od.case_decision_id where od.decision_type = 'ADJOURN')" +
                    " select distinct cast(cd.id as varchar) as id , cd.urn as urn, cdn.saved_at as lastUpdatedDate, cd.prosecuting_authority as prosecutingAuthority, sess.magistrate as magistrate, " +
                    " cast(case " +
                    " when sess.legal_adviser_user_id is null then sess.user_id " +
                    " when sess.legal_adviser_user_id is not null then sess.legal_adviser_user_id " +
                    " end as varchar) as legalAdvisorUserId " +
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

    public CaseDetail findByUrn(final String urn) {
        return entityManager.createQuery("FROM CaseDetail cd WHERE UPPER(cd.urn) = UPPER(:urn)", CaseDetail.class)
                .setParameter("urn", urn)
                .getSingleResult();
    }

    public CaseDetail findByUrnPostcode(final String urn, final String postcode) {
        return entityManager.createQuery("SELECT cd FROM CaseDetail cd " +
                        "INNER JOIN cd.defendant dd " +
                        "WHERE (UPPER(cd.urn) = UPPER(:urn) OR UPPER(REGEXP_REPLACE(cd.urn, '^[a-zA-Z]+', '')) = UPPER(:urn)) " +
                        "AND UPPER(REPLACE(dd.address.postcode,' ','')) = UPPER(REPLACE(:postcode, ' ',''))", CaseDetail.class)
                .setParameter("urn", urn)
                .setParameter("postcode", postcode)
                .getResultList().stream().findFirst().orElse(null);
    }

    public List<CaseDetail> findByDefendantId(final UUID defendantId) {
        return entityManager.createQuery("select cd from CaseDetail cd INNER JOIN cd.defendant dd WHERE dd.id = :defendantId", CaseDetail.class)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }

    public TypedQuery<CaseDetail> findCasesMissingSjpn(final String prosecutingAuthorityFilter,
                                                       final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery("SELECT cd FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
                        "WHERE cdocs IS NULL AND cd.completed IS NOT true AND ( cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter OR cd.prosecutingAuthority IN (:agentProsecutorAuthorityAccess))", CaseDetail.class)
                .setParameter("prosecutingAuthorityFilter", prosecutingAuthorityFilter)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess);
    }

    public TypedQuery<CaseDetail> findCasesMissingSjpn(final String prosecutingAuthorityFilter, final LocalDate postedBefore,
                                                       final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery("SELECT cd FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
                        "WHERE cdocs IS NULL AND cd.postingDate < :postedBefore AND cd.completed IS NOT true AND ( cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter " +
                        "OR cd.prosecutingAuthority IN (:agentProsecutorAuthorityAccess))", CaseDetail.class)
                .setParameter("prosecutingAuthorityFilter", prosecutingAuthorityFilter)
                .setParameter("postedBefore", postedBefore)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess);
    }

    public int countCasesMissingSjpn(final String prosecutingAuthorityFilter,
                                     final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery("SELECT COUNT(cd) FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
                        "WHERE cdocs IS NULL AND cd.completed IS NOT true AND ( cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter OR cd.prosecutingAuthority IN (:agentProsecutorAuthorityAccess) )", Long.class)
                .setParameter("prosecutingAuthorityFilter", prosecutingAuthorityFilter)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getSingleResult().intValue();
    }

    public int countCasesMissingSjpn(final String prosecutingAuthorityFilter, final LocalDate postedBefore,
                                     final List<String> agentProsecutorAuthorityAccess) {
        return entityManager.createQuery("SELECT COUNT(cd) FROM CaseDetail cd LEFT OUTER JOIN cd.caseDocuments cdocs ON cdocs.documentType = 'SJPN' " +
                        "WHERE cdocs IS NULL AND cd.postingDate < :postedBefore AND cd.completed IS NOT true AND ( cd.prosecutingAuthority LIKE :prosecutingAuthorityFilter OR cd.prosecutingAuthority IN (:agentProsecutorAuthorityAccess))", Long.class)
                .setParameter("prosecutingAuthorityFilter", prosecutingAuthorityFilter)
                .setParameter("postedBefore", postedBefore)
                .setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess)
                .getSingleResult().intValue();
    }

    public List<CaseDocument> findCaseDocuments(final UUID caseId) {
        return entityManager.createQuery("SELECT cd.caseDocuments FROM CaseDetail cd where cd.id = :caseId", CaseDocument.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public DefendantDetail findCaseDefendant(final UUID caseId) {
        return entityManager.createQuery("SELECT cd.defendant FROM CaseDetail cd where cd.id = :caseId", DefendantDetail.class)
                .setParameter("caseId", caseId)
                .getSingleResult();
    }

    public CaseDetail findByMaterialId(final UUID materialId) {
        return entityManager.createQuery("select cd from CaseDetail cd JOIN cd.caseDocuments cdocs WHERE cdocs.materialId = :materialId", CaseDetail.class)
                .setParameter("materialId", materialId)
                .getSingleResult();
    }

    public String getProsecutingAuthority(final UUID caseId) {
        return entityManager.createQuery("SELECT cd.prosecutingAuthority FROM CaseDetail cd WHERE cd.id = :caseId", String.class)
                .setParameter("caseId", caseId)
                .getResultList().stream().findFirst().orElse(null);
    }

    public List<PendingCaseToPublishPerOffence> findPublicTransparencyReportPendingCases() {
        return entityManager.createQuery("SELECT new uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence" +
                "(d.personalDetails.title, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, d.personalDetails.dateOfBirth," +
                "cd.id, cd.urn," +
                "d.address.address1, d.address.address2," +
                "d.address.address3, d.address.address4, d.address.address5," +
                "d.address.postcode, o.code, o.startDate, o.wording, " +
                "o.pressRestriction.requested, o.pressRestriction.name, o.completed, cd.prosecutingAuthority, o.wordingWelsh) " +
                "FROM CaseDetail cd " +
                "LEFT OUTER JOIN cd.defendant d " +
                "LEFT OUTER JOIN d.offences o " +
                "WHERE cd.id IN (SELECT rc.id FROM ReadyCase rc) " +
                "AND cd.id IN (SELECT cps.caseId FROM CasePublishStatus cps WHERE cps.numberOfPublishes < 5)" +
                "ORDER BY cd.postingDate", PendingCaseToPublishPerOffence.class)
                .getResultList();
    }

    public List<PendingCaseToPublishPerOffence> findPublicTransparencyDeltaReportPendingCases(final LocalDate fromDate, final LocalDate toDate) {
        return entityManager.createQuery("SELECT new uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence" +
                "(d.personalDetails.title, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, d.personalDetails.dateOfBirth," +
                "cd.id, cd.urn," +
                "d.address.address1, d.address.address2," +
                "d.address.address3, d.address.address4, d.address.address5," +
                "d.address.postcode, o.code, o.startDate, o.wording, " +
                "o.pressRestriction.requested, o.pressRestriction.name, o.completed, cd.prosecutingAuthority, o.wordingWelsh) " +
                "FROM CaseDetail cd " +
                "LEFT OUTER JOIN cd.defendant d " +
                "LEFT OUTER JOIN d.offences o " +
                "WHERE cd.id IN (SELECT rc.id FROM ReadyCase rc WHERE rc.markedAt BETWEEN :fromDate AND :toDate) " +
                "ORDER BY cd.postingDate", PendingCaseToPublishPerOffence.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    public List<PendingCaseToPublishPerOffence> findPressTransparencyReportPendingCases() {
        return entityManager.createQuery("SELECT new uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence" +
                "(d.personalDetails.title, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, d.personalDetails.dateOfBirth," +
                "cd.id, cd.urn," +
                "d.address.address1, d.address.address2," +
                "d.address.address3, d.address.address4, d.address.address5," +
                "d.address.postcode, o.code, o.startDate, o.wording," +
                "o.pressRestriction.requested, o.pressRestriction.name, o.completed, cd.prosecutingAuthority, o.wordingWelsh) " +
                "FROM CaseDetail cd " +
                "LEFT OUTER JOIN cd.defendant d " +
                "LEFT OUTER JOIN d.offences o " +
                "WHERE cd.id IN (SELECT rc.id FROM ReadyCase rc) " +
                "AND cd.id IN (SELECT cps.caseId FROM CasePublishStatus cps WHERE cps.numberOfPublishes < 5)" +
                "ORDER BY cd.postingDate", PendingCaseToPublishPerOffence.class)
                .getResultList();
    }

    public List<PendingCaseToPublishPerOffence> findPressTransparencyDeltaReportPendingCases(final LocalDate fromDate, final LocalDate toDate) {
        return entityManager.createQuery("SELECT new uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence" +
                "(d.personalDetails.title, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, d.personalDetails.dateOfBirth," +
                "cd.id, cd.urn," +
                "d.address.address1, d.address.address2," +
                "d.address.address3, d.address.address4, d.address.address5," +
                "d.address.postcode, o.code, o.startDate, o.wording," +
                "o.pressRestriction.requested, o.pressRestriction.name, o.completed, cd.prosecutingAuthority, o.wordingWelsh) " +
                "FROM CaseDetail cd " +
                "LEFT OUTER JOIN cd.defendant d " +
                "LEFT OUTER JOIN d.offences o " +
                "WHERE cd.id IN (SELECT rc.id FROM ReadyCase rc WHERE rc.markedAt BETWEEN :fromDate AND :toDate) " +
                "ORDER BY cd.postingDate", PendingCaseToPublishPerOffence.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    public List<CaseNotGuiltyPlea> findCasesNotGuiltyPleaByProsecutingAuthority(final String prosecutingAuthority) {
        return entityManager.createQuery("SELECT DISTINCT new uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea" +
                "(e.id, e.urn, o.pleaDate, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, e.prosecutingAuthority, e.caseManagementStatus) " +
                "FROM CaseDetail e " +
                "JOIN e.defendant d " +
                "JOIN d.offences o " +
                "WHERE e.completed = false " +
                "AND o.plea = 'NOT_GUILTY' " +
                "AND e.caseStatus != 'REFER_FOR_COURT_HEARING' " +
                "AND e.prosecutingAuthority = :prosecutingAuthority " +
                "ORDER BY o.pleaDate DESC ", CaseNotGuiltyPlea.class)
                .setParameter("prosecutingAuthority", prosecutingAuthority)
                .getResultList();
    }

    public List<CaseNotGuiltyPlea> findCasesNotGuiltyPlea() {
        return entityManager.createQuery("SELECT DISTINCT new uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea" +
                "(e.id, e.urn, o.pleaDate, d.personalDetails.firstName, d.personalDetails.lastName, d.legalEntityDetails.legalEntityName, e.prosecutingAuthority, e.caseManagementStatus) " +
                "FROM CaseDetail e " +
                "JOIN e.defendant d " +
                "JOIN d.offences o " +
                "WHERE e.completed = false " +
                "AND o.plea = 'NOT_GUILTY' " +
                "AND e.caseStatus != 'REFER_FOR_COURT_HEARING' " +
                "ORDER BY o.pleaDate DESC ", CaseNotGuiltyPlea.class)
                .getResultList();
    }

    public List<CaseWithoutDefendantPostcode> findCasesWithoutDefendantPostcode() {
        return entityManager.createQuery("SELECT DISTINCT new uk.gov.moj.cpp.sjp.persistence.entity.CaseWithoutDefendantPostcode" +
                "(e.id, e.urn, e.postingDate, d.personalDetails.firstName, d.personalDetails.lastName, e.prosecutingAuthority,d.legalEntityDetails.legalEntityName) " +
                "FROM CaseDetail e " +
                "JOIN e.defendant d " +
                "WHERE e.completed = false " +
                "AND d.address.postcode IS NULL " +
                "ORDER BY e.postingDate DESC ", CaseWithoutDefendantPostcode.class)
                .getResultList();
    }

    public void updateDatesToAvoid(final UUID caseId, final String datesToAvoid) {
        findBy(caseId).setDatesToAvoid(datesToAvoid);
    }

    public CaseDetail findBy(final UUID id) {
        return entityManager.find(CaseDetail.class, id);
    }

    public CaseDetail save(final CaseDetail caseDetail) {
        return entityManager.merge(caseDetail);
    }

    public void remove(final CaseDetail caseDetail) {
        entityManager.remove(entityManager.contains(caseDetail) ? caseDetail : entityManager.merge(caseDetail));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(cd) FROM CaseDetail cd", Long.class).getSingleResult();
    }
}
