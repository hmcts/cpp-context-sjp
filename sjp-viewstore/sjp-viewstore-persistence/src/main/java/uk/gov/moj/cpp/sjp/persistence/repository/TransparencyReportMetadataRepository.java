package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface TransparencyReportMetadataRepository extends EntityRepository<TransparencyReportMetadata, UUID> {

    @Query(value = "SELECT trmd FROM TransparencyReportMetadata trmd " +
            "WHERE trmd.fileServiceId is not null and trmd.generatedAt > :fromDate " +
            "ORDER BY trmd.generatedAt DESC")
    List<TransparencyReportMetadata> findLatestTransparencyReportMetadata(@QueryParam("fromDate") LocalDateTime fromDate);

}
