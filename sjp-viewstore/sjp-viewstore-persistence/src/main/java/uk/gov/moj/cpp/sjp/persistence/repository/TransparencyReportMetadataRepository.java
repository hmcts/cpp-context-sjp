package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TransparencyReportMetadataRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<TransparencyReportMetadata> findLatestTransparencyReportMetadata(LocalDateTime fromDate) {
        return entityManager.createQuery(
                "SELECT trmd FROM TransparencyReportMetadata trmd " +
                        "WHERE trmd.fileServiceId is not null and trmd.generatedAt > :fromDate " +
                        "ORDER BY trmd.generatedAt DESC",
                TransparencyReportMetadata.class)
                .setParameter("fromDate", fromDate)
                .getResultList();
    }

    public TransparencyReportMetadata findBy(final UUID id) {
        return entityManager.find(TransparencyReportMetadata.class, id);
    }

    public TransparencyReportMetadata save(final TransparencyReportMetadata entity) {
        return entityManager.merge(entity);
    }

    public void remove(final TransparencyReportMetadata entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM TransparencyReportMetadata e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<TransparencyReportMetadata> findAll() {
        return entityManager.createQuery("SELECT e FROM TransparencyReportMetadata e", TransparencyReportMetadata.class).getResultList();
    }
}
