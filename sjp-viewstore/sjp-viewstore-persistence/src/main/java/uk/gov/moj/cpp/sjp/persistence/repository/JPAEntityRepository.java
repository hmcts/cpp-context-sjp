package uk.gov.moj.cpp.sjp.persistence.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

public abstract class JPAEntityRepository<T, K> {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    protected EntityManager entityManager;

    protected abstract Class<T> getEntityClass();

    public T save(final T entity) {
        final Object id = entityManager.getEntityManagerFactory()
                .getPersistenceUnitUtil().getIdentifier(entity);

        if (id != null && entityManager.find(getEntityClass(), id) != null) {
            return entityManager.merge(entity);
        }
        entityManager.persist(entity);
        return entity;
    }

    public T findBy(final K id) {
        return entityManager.find(getEntityClass(), id);
    }

    public List<T> findAll() {
        final CriteriaQuery<T> query = entityManager.getCriteriaBuilder()
                .createQuery(getEntityClass());
        query.select(query.from(getEntityClass()));
        return entityManager.createQuery(query).getResultList();
    }

    public Long count() {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = cb.createQuery(Long.class);
        query.select(cb.count(query.from(getEntityClass())));
        return entityManager.createQuery(query).getSingleResult();
    }

    public void remove(final T entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }
}
