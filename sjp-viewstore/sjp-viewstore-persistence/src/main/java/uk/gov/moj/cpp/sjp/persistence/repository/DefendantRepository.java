package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;

import javax.enterprise.context.ApplicationScoped;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link DefendantDetail}
 */
@ApplicationScoped
public class DefendantRepository extends JPAEntityRepository<DefendantDetail, UUID>{

    @Override
    protected Class<DefendantDetail> getEntityClass() {
        return DefendantDetail.class;
    }

    private static final String UPDATED_DEFENDANT_DETAILS = "SELECT new uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails(" +
            "dd.personalDetails.firstName, " +
            "dd.personalDetails.lastName, " +
            "dd.personalDetails.dateOfBirth, " +
            "dd.id, " +
            "dd.addressUpdatedAt, " +
            "dd.personalDetails.dateOfBirthUpdatedAt, " +
            "dd.nameUpdatedAt, " +
            "cd.urn, " +
            "cd.id, " +
            "dd.region," +
            "dd.legalEntityDetails.legalEntityName,"+
            "cd.prosecutingAuthority"+
            ") FROM DefendantDetail dd " +
            "INNER JOIN dd.caseDetail cd " +
            "WHERE (cd.prosecutingAuthority LIKE :prosecutingAuthority OR cd.prosecutingAuthority IN (:agentProsecutorAuthorityAccess)) " +
            "AND (((dd.addressUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.addressUpdatedAt IS NOT NULL AND dd.updatesAcknowledgedAt is NULL OR dd.addressUpdatedAt > dd.updatesAcknowledgedAt)) " +
            "OR ((dd.personalDetails.dateOfBirthUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.personalDetails.dateOfBirthUpdatedAt IS NOT NULL AND dd.updatesAcknowledgedAt IS NULL OR dd.personalDetails.dateOfBirthUpdatedAt > dd.updatesAcknowledgedAt)) " +
            "OR ((dd.nameUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.nameUpdatedAt IS NOT NULL AND dd.updatesAcknowledgedAt IS NULL OR dd.nameUpdatedAt > dd.updatesAcknowledgedAt)))";

    public List<UpdatedDefendantDetails> findUpdatedByCaseProsecutingAuthority(
            String prosecutingAuthority,
            ZonedDateTime fromDate,
            ZonedDateTime toDate,
            List<String> agentProsecutorAuthorityAccess) {

        if (agentProsecutorAuthorityAccess == null || agentProsecutorAuthorityAccess.isEmpty()) {
            agentProsecutorAuthorityAccess = Collections.singletonList("DUMMY_VALUE");
        }

        final javax.persistence.Query query = entityManager.createQuery(UPDATED_DEFENDANT_DETAILS);

        query.setParameter("prosecutingAuthority", prosecutingAuthority);
        query.setParameter("agentProsecutorAuthorityAccess", agentProsecutorAuthorityAccess);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        return query.getResultList();
    }

    public UUID findCaseIdByDefendantId(final UUID id) {
        return entityManager.createQuery("SELECT d.caseDetail.id FROM DefendantDetail d WHERE d.id=:id", UUID.class)
                .setParameter("id", id)
                .getResultStream().findFirst().orElse(null);
    }

    public UUID findCaseIdByCorrelationId(final UUID correlationId) {
        return entityManager.createQuery("SELECT d.caseDetail.id FROM DefendantDetail d WHERE d.correlationId=:correlationId", UUID.class)
                .setParameter("correlationId", correlationId)
                .getResultStream().findFirst().orElse(null);
    }

    public UUID findOptionalCaseIdByDefendantId(final UUID id) {
        return entityManager.createQuery("SELECT d.caseDetail.id FROM DefendantDetail d WHERE d.id=:id", UUID.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public List<DefendantDetail> findByReadyCases() {
        return entityManager.createQuery(
                "SELECT dd FROM DefendantDetail dd, ReadyCase rc where dd.caseDetail.id = rc.caseId",
                DefendantDetail.class)
                .getResultList();
    }
}
