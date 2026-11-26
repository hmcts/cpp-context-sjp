package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class AocpAcceptedEmailNotificationStatusRepository extends JPAEntityRepository<AocpAcceptedEmailStatus, UUID>{

    @Override
    protected Class<AocpAcceptedEmailStatus> getEntityClass() {
        return AocpAcceptedEmailStatus.class;
    }
}
