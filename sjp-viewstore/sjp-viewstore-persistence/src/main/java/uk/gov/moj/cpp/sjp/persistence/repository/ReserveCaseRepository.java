package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
                .getSingleResult()
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
        return entityManager.createQuery("SELECT COUNT(e) FROM ReserveCase e", Long.class).getSingleResult();
    }

    public List<ReserveCase> findAll() {
        return entityManager.createQuery("SELECT e FROM ReserveCase e", ReserveCase.class).getResultList();
    }
}
