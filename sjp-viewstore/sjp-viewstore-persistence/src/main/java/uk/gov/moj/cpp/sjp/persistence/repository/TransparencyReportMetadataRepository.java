package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TransparencyReportMetadataRepository extends JPAEntityRepository<TransparencyReportMetadata, UUID> {

    @Override
    protected Class<TransparencyReportMetadata> getEntityClass() {
        return TransparencyReportMetadata.class;
    }

    public List<TransparencyReportMetadata> findLatestTransparencyReportMetadata(LocalDateTime fromDate) {
        return entityManager.createQuery(
                "SELECT trmd FROM TransparencyReportMetadata trmd " +
                        "WHERE trmd.fileServiceId is not null and trmd.generatedAt > :fromDate " +
                        "ORDER BY trmd.generatedAt DESC",
                TransparencyReportMetadata.class)
                .setParameter("fromDate", fromDate)
                .getResultList();
    }
}
