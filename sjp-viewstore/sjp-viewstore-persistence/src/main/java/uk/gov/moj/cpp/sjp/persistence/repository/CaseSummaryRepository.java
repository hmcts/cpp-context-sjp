package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CaseSummaryRepository extends JPAEntityRepository<CaseSummary, UUID> {

    @Override
    protected Class<CaseSummary> getEntityClass() {
        return CaseSummary.class;
    }
}