package uk.gov.moj.cpp.sjp.persistence.repository;

import java.util.UUID;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;

@Repository
public interface AocpAcceptedEmailNotificationStatusRepository extends EntityRepository<AocpAcceptedEmailStatus, UUID> {
}
