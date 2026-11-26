package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CaseApplicationDecisionRepository extends JPAEntityRepository<CaseApplicationDecision, UUID> {

    @Override
    protected Class<CaseApplicationDecision> getEntityClass() {
        return CaseApplicationDecision.class;
    }
}
