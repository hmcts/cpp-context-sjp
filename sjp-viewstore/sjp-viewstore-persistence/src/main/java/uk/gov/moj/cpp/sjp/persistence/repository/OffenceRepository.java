package uk.gov.moj.cpp.sjp.persistence.repository;


import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link OffenceDetail}
 */
@Repository
public interface OffenceRepository extends EntityRepository<OffenceDetail, UUID> {

    List<OffenceDetail> findByDefendantDetail(DefendantDetail defendantDetail);
}
