package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CaseApplicationRepository extends JPAEntityRepository<CaseApplication, UUID> {

    @Override
    protected Class<CaseApplication> getEntityClass() {
        return CaseApplication.class;
    }

    public CaseDetail findByApplicationDecisionId(final UUID applicationDecisionId) {
        return entityManager.createQuery("SELECT ca.caseDetail FROM CaseApplication ca WHERE ca.applicationDecision.decisionId=:applicationDecisionId", CaseDetail.class)
                .setParameter("applicationDecisionId", applicationDecisionId)
                .getResultStream().findFirst().orElse(null);
    }

    public CaseDetail findByApplicationId(final UUID applicationId) {
        return entityManager.createQuery("SELECT ca.caseDetail FROM CaseApplication ca WHERE ca.id=:applicationId", CaseDetail.class)
                .setParameter("applicationId", applicationId)
                .getResultStream().findFirst().orElse(null);
    }
}
