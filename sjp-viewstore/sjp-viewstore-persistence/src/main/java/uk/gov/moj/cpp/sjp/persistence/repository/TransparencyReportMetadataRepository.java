package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface TransparencyReportMetadataRepository extends EntityRepository<TransparencyReportMetadata, UUID> {

    @Query(value = "SELECT trmd FROM TransparencyReportMetadata trmd " +
            "WHERE trmd.englishFileServiceId is not null and trmd.welshFileServiceId is not null " +
            "ORDER BY trmd.generatedAt DESC", max = 1)
    TransparencyReportMetadata findLatestTransparencyReportMetadata();

}
