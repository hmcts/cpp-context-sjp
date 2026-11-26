package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link OffenceDetail}
 */
@ApplicationScoped
public class OffenceRepository extends JPAEntityRepository<OffenceDetail, UUID>{

    @Override
    protected Class<OffenceDetail> getEntityClass() {
        return OffenceDetail.class;
    }

    public List<OffenceDetail> findByDefendantDetail(DefendantDetail defendantDetail) {
        return entityManager.createQuery(
                "SELECT o FROM OffenceDetail o WHERE o.defendantDetail = :defendantDetail",
                OffenceDetail.class)
                .setParameter("defendantDetail", defendantDetail)
                .getResultList();
    }

    public List<OffenceDetail> findByIds(List<UUID> offenceIds) {
        return entityManager.createQuery(
                "SELECT od FROM OffenceDetail od WHERE od.id IN (:offenceIds)",
                OffenceDetail.class)
                .setParameter("offenceIds", offenceIds)
                .getResultList();
    }
}
