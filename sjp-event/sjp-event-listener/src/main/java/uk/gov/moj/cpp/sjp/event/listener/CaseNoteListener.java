package uk.gov.moj.cpp.sjp.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseNoteRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class CaseNoteListener {

    @Inject
    private CaseNoteRepository caseNoteRepository;

    @Handles("sjp.events.case-note-added")
    public void handleCaseNoteAdded(final Envelope<CaseNoteAdded> envelope) {
        final CaseNoteAdded caseNoteAdded = envelope.payload();
        final CaseNote caseNote = new CaseNote();

        caseNote.setCaseId(caseNoteAdded.getCaseId());
        caseNote.setNoteId(caseNoteAdded.getNote().getId());
        caseNote.setNoteType(caseNoteAdded.getNote().getType());
        caseNote.setNoteText(caseNoteAdded.getNote().getText());
        caseNote.setAddedAt(caseNoteAdded.getNote().getAddedAt().toLocalDateTime());
        caseNote.setAuthorUserId(caseNoteAdded.getAuthor().getUserId());
        caseNote.setAuthorFirstName(caseNoteAdded.getAuthor().getFirstName());
        caseNote.setAuthorLastName(caseNoteAdded.getAuthor().getLastName());
        caseNote.setDecisionId(caseNoteAdded.getDecisionId());

        caseNoteRepository.save(caseNote);
    }
}
