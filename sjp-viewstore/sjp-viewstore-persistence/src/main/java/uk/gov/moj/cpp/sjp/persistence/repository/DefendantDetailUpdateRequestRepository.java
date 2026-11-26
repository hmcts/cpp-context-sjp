package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class DefendantDetailUpdateRequestRepository extends JPAEntityRepository<DefendantDetailUpdateRequest, UUID>{

    @Override
    protected Class<DefendantDetailUpdateRequest> getEntityClass() {
        return DefendantDetailUpdateRequest.class;
    }
}
