package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReserveCaseRepository extends JPAEntityRepository<ReserveCase, UUID> {

    @Override
    protected Class<ReserveCase> getEntityClass() {
        return ReserveCase.class;
    }

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
}
