package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.SocCheck;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

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
        return entityManager.createQuery("SELECT COUNT(e) FROM SocCheck e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<SocCheck> findAll() {
        return entityManager.createQuery("SELECT e FROM SocCheck e", SocCheck.class).getResultList();
    }
}
