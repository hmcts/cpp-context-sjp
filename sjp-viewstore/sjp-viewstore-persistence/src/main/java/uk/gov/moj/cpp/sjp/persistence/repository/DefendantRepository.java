package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.SingleResultType;

/**
 * Repository for {@link DefendantDetail}
 */
@Repository
public abstract class DefendantRepository implements EntityRepository<DefendantDetail, UUID> {

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
            "dd.legalEntityDetails.legalEntityName"+
            ") FROM DefendantDetail dd " +
            "INNER JOIN dd.caseDetail cd " +
            "WHERE cd.prosecutingAuthority LIKE :prosecutingAuthority " +
            "AND (((dd.addressUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.addressUpdatedAt IS NOT NULL AND dd.updatesAcknowledgedAt is NULL OR dd.addressUpdatedAt > dd.updatesAcknowledgedAt)) " +
            "OR ((dd.personalDetails.dateOfBirthUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.personalDetails.dateOfBirthUpdatedAt IS NOT NULL AND dd.updatesAcknowledgedAt IS NULL OR dd.personalDetails.dateOfBirthUpdatedAt > dd.updatesAcknowledgedAt)) " +
            "OR ((dd.nameUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.nameUpdatedAt IS NOT NULL AND dd.updatesAcknowledgedAt IS NULL OR dd.nameUpdatedAt > dd.updatesAcknowledgedAt)))";
    @Inject
    private EntityManager entityManager;

    public List<UpdatedDefendantDetails> findUpdatedByCaseProsecutingAuthority(
            String prosecutingAuthority,
            ZonedDateTime fromDate,
            ZonedDateTime toDate) {

        final javax.persistence.Query query = entityManager.createQuery(UPDATED_DEFENDANT_DETAILS);

        query.setParameter("prosecutingAuthority", prosecutingAuthority);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        return query.getResultList();
    }

    @Query("SELECT d.caseDetail.id FROM DefendantDetail d WHERE d.id=:id")
    public abstract UUID findCaseIdByDefendantId(@QueryParam("id") final UUID id);

    @Query("SELECT d.caseDetail.id FROM DefendantDetail d WHERE d.correlationId=:correlationId")
    public abstract UUID findCaseIdByCorrelationId(@QueryParam("correlationId") final UUID correlationId);

    @Query(value = "SELECT d.caseDetail.id FROM DefendantDetail d WHERE d.id=:id",
            singleResult = SingleResultType.OPTIONAL)
    public abstract UUID findOptionalCaseIdByDefendantId(@QueryParam("id") final UUID id);

    @Query(value = "SELECT dd FROM DefendantDetail dd, ReadyCase rc where dd.caseDetail.id = rc.caseId")
    public abstract List<DefendantDetail> findByReadyCases();
}
