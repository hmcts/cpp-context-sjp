package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SessionRepository extends JPAEntityRepository<Session, UUID>{

    @Override
    protected Class<Session> getEntityClass() {
        return Session.class;
    }

    public List<Session> findLatestAocpSession() {
        return entityManager.createQuery(
                "select session1 from Session session1 where session1.type='AOCP' and session1.endedAt is null and session1.startedAt =" +
                        "(select max(session2.startedAt) from Session session2 where session2.type='AOCP' and session2.endedAt is null)",
                Session.class)
                .getResultList();
    }
}
