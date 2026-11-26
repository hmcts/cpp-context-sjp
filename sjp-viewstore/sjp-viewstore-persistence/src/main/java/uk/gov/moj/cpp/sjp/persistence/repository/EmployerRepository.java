package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.Employer;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class EmployerRepository extends JPAEntityRepository<Employer, UUID> {

    @Override
    protected Class<Employer> getEntityClass() {
        return Employer.class;
    }
}
