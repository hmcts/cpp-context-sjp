package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.SocCheck;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class SocCheckRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public SocCheck findBy(final UUID id) {
        return entityManager.find(SocCheck.class, id);
    }

    public SocCheck save(final SocCheck entity) {
        return entityManager.merge(entity);
    }

    public void remove(final SocCheck entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM SocCheck e", Long.class).getSingleResult();
    }

    public List<SocCheck> findAll() {
        return entityManager.createQuery("SELECT e FROM SocCheck e", SocCheck.class).getResultList();
    }
}
