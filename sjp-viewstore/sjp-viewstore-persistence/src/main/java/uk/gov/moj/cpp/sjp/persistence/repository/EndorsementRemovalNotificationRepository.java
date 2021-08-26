package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface EndorsementRemovalNotificationRepository extends EntityRepository<NotificationOfEndorsementStatus, UUID> {
}
