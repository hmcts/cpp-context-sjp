package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface PressTransparencyReportMetadataRepository extends EntityRepository<PressTransparencyReportMetadata, UUID> {

    @Query(value = "SELECT ptrmd FROM PressTransparencyReportMetadata ptrmd " +
            "WHERE ptrmd.fileServiceId is not null and ptrmd.generatedAt > :fromDate " +
            "ORDER BY ptrmd.generatedAt DESC")
    List<PressTransparencyReportMetadata> findLatestPressTransparencyReportMetadata(@QueryParam("fromDate") LocalDateTime fromDate);
}
