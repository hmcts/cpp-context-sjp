package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class EnforcementPendingApplicationNotificationStatusRepository extends JPAEntityRepository<EnforcementNotification,UUID> {

    @Override
    protected Class<EnforcementNotification> getEntityClass() {
        return EnforcementNotification.class;
    }
}
