package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link OffenceDetail}
 */
@Repository
public interface OffenceRepository extends EntityRepository<OffenceDetail, UUID> {

    List<OffenceDetail> findByDefendantDetail(DefendantDetail defendantDetail);

    @Query("FROM OffenceDetail od WHERE od.id IN :offenceIds")
    List<OffenceDetail> findByIds(@QueryParam("offenceIds") List<UUID> offenceIds);

}
