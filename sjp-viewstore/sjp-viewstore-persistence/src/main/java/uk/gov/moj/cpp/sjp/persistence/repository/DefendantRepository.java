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

    @Inject
    private EntityManager entityManager;

    private static final String UPDATED_DEFENDANT_DETAILS = "SELECT new uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails(" +
            "dd.personalDetails.firstName, " +
            "dd.personalDetails.lastName, " +
            "dd.personalDetails.dateOfBirth, " +
            "dd.id, " +
            "dd.personalDetails.addressUpdatedAt, " +
            "dd.personalDetails.dateOfBirthUpdatedAt, " +
            "dd.personalDetails.nameUpdatedAt, " +
            "cd.urn, " +
            "cd.id" +
            ") FROM DefendantDetail dd " +
            "INNER JOIN dd.caseDetail cd " +
            "WHERE cd.prosecutingAuthority LIKE :prosecutingAuthority " +
            "AND (((dd.personalDetails.addressUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.personalDetails.addressUpdatedAt IS NOT NULL AND dd.personalDetails.updatesAcknowledgedAt is NULL OR dd.personalDetails.addressUpdatedAt > dd.personalDetails.updatesAcknowledgedAt)) " +
            "OR ((dd.personalDetails.dateOfBirthUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.personalDetails.dateOfBirthUpdatedAt IS NOT NULL AND dd.personalDetails.updatesAcknowledgedAt IS NULL OR dd.personalDetails.dateOfBirthUpdatedAt > dd.personalDetails.updatesAcknowledgedAt)) " +
            "OR ((dd.personalDetails.nameUpdatedAt BETWEEN :fromDate and :toDate) AND (dd.personalDetails.nameUpdatedAt IS NOT NULL AND dd.personalDetails.updatesAcknowledgedAt IS NULL OR dd.personalDetails.nameUpdatedAt > dd.personalDetails.updatesAcknowledgedAt)))";

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

    @Query(value = "SELECT d.caseDetail.id FROM DefendantDetail d WHERE d.id=:id",
            singleResult = SingleResultType.OPTIONAL)
    public abstract UUID findOptionalCaseIdByDefendantId(@QueryParam("id") final UUID id);

}
