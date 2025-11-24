package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseNoteRepository extends EntityRepository<CaseNote, UUID> {

    List<CaseNote> findByCaseIdOrderByAddedAtDesc(final UUID caseId);

    List<CaseNote> findByCaseIdAndNoteTypeOrderByAddedAtDesc(final UUID caseId, final NoteType noteType);
}