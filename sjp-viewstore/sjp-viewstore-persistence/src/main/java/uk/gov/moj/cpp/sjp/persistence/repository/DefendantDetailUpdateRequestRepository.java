package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantDetailUpdateRequestRepository extends EntityRepository<DefendantDetailUpdateRequest, UUID> {

}