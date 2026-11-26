package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CaseCourtReferralStatusRepository extends JPAEntityRepository<CaseCourtReferralStatus, UUID> {

    @Override
    protected Class<CaseCourtReferralStatus> getEntityClass() {
        return CaseCourtReferralStatus.class;
    }
}
