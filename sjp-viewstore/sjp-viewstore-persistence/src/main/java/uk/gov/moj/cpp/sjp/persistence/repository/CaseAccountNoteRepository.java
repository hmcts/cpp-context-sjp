package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.AccountNote;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseAccountNoteRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<AccountNote> findByCaseUrn(final String caseUrn) {
        return entityManager.createQuery("SELECT e FROM AccountNote e WHERE e.caseUrn = :caseUrn", AccountNote.class)
                .setParameter("caseUrn", caseUrn)
                .getResultList();
    }

    public AccountNote findBy(final UUID id) {
        return entityManager.find(AccountNote.class, id);
    }

    public AccountNote save(final AccountNote entity) {
        return entityManager.merge(entity);
    }

    public void remove(final AccountNote entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM AccountNote e", Long.class).getSingleResult();
    }

    public List<AccountNote> findAll() {
        return entityManager.createQuery("SELECT e FROM AccountNote e", AccountNote.class).getResultList();
    }
}
