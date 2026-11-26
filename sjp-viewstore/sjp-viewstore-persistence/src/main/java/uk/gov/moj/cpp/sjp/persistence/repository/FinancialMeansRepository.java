package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class FinancialMeansRepository extends JPAEntityRepository<FinancialMeans ,UUID> {

    @Override
    protected Class<FinancialMeans> getEntityClass() {
        return FinancialMeans.class;
    }
}
