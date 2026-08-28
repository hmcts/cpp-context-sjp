package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository for {@link OffenceDetail}
 */
@ApplicationScoped
public class OffenceRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<OffenceDetail> findByDefendantDetail(DefendantDetail defendantDetail) {
        return entityManager.createQuery(
                "SELECT o FROM OffenceDetail o WHERE o.defendantDetail = :defendantDetail",
                OffenceDetail.class)
                .setParameter("defendantDetail", defendantDetail)
                .getResultList();
    }

    public List<OffenceDetail> findByIds(List<UUID> offenceIds) {
        return entityManager.createQuery(
                "FROM OffenceDetail od WHERE od.id IN :offenceIds",
                OffenceDetail.class)
                .setParameter("offenceIds", offenceIds)
                .getResultList();
    }

    public OffenceDetail findBy(final UUID id) {
        return entityManager.find(OffenceDetail.class, id);
    }

    public OffenceDetail save(final OffenceDetail entity) {
        return entityManager.merge(entity);
    }

    public void remove(final OffenceDetail entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM OffenceDetail e", Long.class).getSingleResult();
    }

    public List<OffenceDetail> findAll() {
        return entityManager.createQuery("SELECT e FROM OffenceDetail e", OffenceDetail.class).getResultList();
    }
}
