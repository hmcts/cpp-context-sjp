package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.SocCheck;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SocCheckRepository extends JPAEntityRepository<SocCheck, UUID> {
    @Override
    protected Class<SocCheck> getEntityClass() {
        return SocCheck.class;
    }
}
