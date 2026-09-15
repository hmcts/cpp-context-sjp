package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class SessionRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<Session> findLatestAocpSession() {
        return entityManager.createQuery(
                "select session1 from Session session1 where session1.type='AOCP' and session1.endedAt is null and session1.startedAt =" +
                        "(select max(session2.startedAt) from Session session2 where session2.type='AOCP' and session2.endedAt is null)",
                Session.class)
                .getResultList();
    }

    public Session findBy(final UUID id) {
        return entityManager.find(Session.class, id);
    }

    public Session save(final Session entity) {
        return entityManager.merge(entity);
    }

    public void remove(final Session entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM Session e", Long.class).getSingleResult();
    }

    public List<Session> findAll() {
        return entityManager.createQuery("SELECT e FROM Session e", Session.class).getResultList();
    }
}
