package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PressTransparencyReportMetadataRepository extends JPAEntityRepository<PressTransparencyReportMetadata, UUID> {

    @Override
    protected Class<PressTransparencyReportMetadata> getEntityClass() {
        return PressTransparencyReportMetadata.class;
    }

    public List<PressTransparencyReportMetadata> findLatestPressTransparencyReportMetadata(LocalDateTime fromDate) {
        return entityManager.createQuery(
                "SELECT ptrmd FROM PressTransparencyReportMetadata ptrmd " +
                        "WHERE ptrmd.fileServiceId is not null and ptrmd.generatedAt > :fromDate " +
                        "ORDER BY ptrmd.generatedAt DESC",
                PressTransparencyReportMetadata.class)
                .setParameter("fromDate", fromDate)
                .getResultList();
    }
}
