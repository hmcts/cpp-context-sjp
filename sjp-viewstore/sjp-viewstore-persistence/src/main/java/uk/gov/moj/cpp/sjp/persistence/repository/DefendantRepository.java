package uk.gov.moj.cpp.sjp.persistence.repository;


import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.util.UUID;

/**
 * Repository for {@link DefendantDetail}
 */
@Repository
public interface DefendantRepository extends EntityRepository<DefendantDetail, UUID> {
}
