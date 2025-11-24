package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface EmailNotificationRepository extends EntityRepository<EmailNotification, UUID> {

    @Query(value = "FROM EmailNotification e WHERE e.referenceId = :referenceId AND e.notificationType = :notificationType")
    EmailNotification findByReferenceIdAndNotificationType(@QueryParam("referenceId") UUID referenceId, @QueryParam("notificationType") EmailNotification.NotificationNotifyDocumentType notificationType);
}
