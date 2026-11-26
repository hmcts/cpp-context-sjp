package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class EndorsementRemovalNotificationRepository extends JPAEntityRepository<NotificationOfEndorsementStatus, UUID> {

    @Override
    protected Class<NotificationOfEndorsementStatus> getEntityClass() {
        return NotificationOfEndorsementStatus.class;
    }
}
