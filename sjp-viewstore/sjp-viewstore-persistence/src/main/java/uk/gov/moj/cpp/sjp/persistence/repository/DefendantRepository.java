package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link DefendantDetail}
 */
@Repository
public interface DefendantRepository extends EntityRepository<DefendantDetail, UUID> {
}
