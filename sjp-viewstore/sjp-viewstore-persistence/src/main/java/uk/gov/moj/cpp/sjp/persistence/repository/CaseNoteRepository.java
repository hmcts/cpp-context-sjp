package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CaseNoteRepository extends JPAEntityRepository<CaseNote, UUID> {

    @Override
    protected Class<CaseNote> getEntityClass() {
        return CaseNote.class;
    }

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
}