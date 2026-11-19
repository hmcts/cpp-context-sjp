package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.Employer;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EmployerRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public Employer findBy(final UUID id) {
        return entityManager.find(Employer.class, id);
    }

    public Employer save(final Employer entity) {
        return entityManager.merge(entity);
    }

    public void remove(final Employer entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM Employer e", Long.class).getResultStream().findFirst().orElse(null);
    }

    public List<Employer> findAll() {
        return entityManager.createQuery("SELECT e FROM Employer e", Employer.class).getResultList();
    }
}
