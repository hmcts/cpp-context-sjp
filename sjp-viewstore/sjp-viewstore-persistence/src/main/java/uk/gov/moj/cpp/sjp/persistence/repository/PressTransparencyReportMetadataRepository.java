package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class PressTransparencyReportMetadataRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<PressTransparencyReportMetadata> findLatestPressTransparencyReportMetadata(LocalDateTime fromDate) {
        return entityManager.createQuery(
                "SELECT ptrmd FROM PressTransparencyReportMetadata ptrmd " +
                        "WHERE ptrmd.fileServiceId is not null and ptrmd.generatedAt > :fromDate " +
                        "ORDER BY ptrmd.generatedAt DESC",
                PressTransparencyReportMetadata.class)
                .setParameter("fromDate", fromDate)
                .getResultList();
    }

    public PressTransparencyReportMetadata findBy(final UUID id) {
        return entityManager.find(PressTransparencyReportMetadata.class, id);
    }

    public PressTransparencyReportMetadata save(final PressTransparencyReportMetadata entity) {
        return entityManager.merge(entity);
    }

    public void remove(final PressTransparencyReportMetadata entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM PressTransparencyReportMetadata e", Long.class).getSingleResult();
    }

    public List<PressTransparencyReportMetadata> findAll() {
        return entityManager.createQuery("SELECT e FROM PressTransparencyReportMetadata e", PressTransparencyReportMetadata.class).getResultList();
    }
}
