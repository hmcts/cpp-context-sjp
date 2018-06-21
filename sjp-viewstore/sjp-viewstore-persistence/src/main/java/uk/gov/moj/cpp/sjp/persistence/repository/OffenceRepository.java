package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link OffenceDetail}
 */
@Repository
public interface OffenceRepository extends EntityRepository<OffenceDetail, UUID> {

    List<OffenceDetail> findByDefendantDetail(DefendantDetail defendantDetail);
}
