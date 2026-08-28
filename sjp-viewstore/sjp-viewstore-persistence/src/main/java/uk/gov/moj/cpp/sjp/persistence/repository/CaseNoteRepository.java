package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseNoteRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    public List<CaseNote> findByCaseIdOrderByAddedAtDesc(final UUID caseId) {
        return entityManager.createQuery("SELECT e FROM CaseNote e WHERE e.caseId = :caseId ORDER BY e.addedAt DESC", CaseNote.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public List<CaseNote> findByCaseIdAndNoteTypeOrderByAddedAtDesc(final UUID caseId, final NoteType noteType) {
        return entityManager.createQuery("SELECT e FROM CaseNote e WHERE e.caseId = :caseId AND e.noteType = :noteType ORDER BY e.addedAt DESC", CaseNote.class)
                .setParameter("caseId", caseId)
                .setParameter("noteType", noteType)
                .getResultList();
    }

    public CaseNote findBy(final UUID id) {
        return entityManager.find(CaseNote.class, id);
    }

    public CaseNote save(final CaseNote entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseNote entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseNote e", Long.class).getSingleResult();
    }

    public List<CaseNote> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseNote e", CaseNote.class).getResultList();
    }
}
