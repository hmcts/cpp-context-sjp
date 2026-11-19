package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;


@ApplicationScoped
public class ReserveCaseRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<ReserveCase> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                "SELECT e FROM ReserveCase e WHERE e.caseId = :caseId",
                ReserveCase.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public int countReservedBy(UUID reservedBy) {
        return entityManager.createQuery(
                "SELECT COUNT(rc) FROM ReserveCase rc where rc.reservedBy=:reservedBy",
                Long.class)
                .setParameter("reservedBy", reservedBy)
                .getResultStream().findFirst().orElse(null)
                .intValue();
    }

    public ReserveCase findBy(final UUID id) {
        return entityManager.find(ReserveCase.class, id);
    }

    public ReserveCase save(final ReserveCase entity) {
        return entityManager.merge(entity);
    }

    public void remove(final ReserveCase entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM ReserveCase e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<ReserveCase> findAll() {
        return entityManager.createQuery("SELECT e FROM ReserveCase e", ReserveCase.class).getResultList();
    }
}
