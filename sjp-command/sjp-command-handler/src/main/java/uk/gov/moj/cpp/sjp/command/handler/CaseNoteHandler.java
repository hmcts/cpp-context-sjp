package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.json.schemas.domains.sjp.AddCaseNote;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class CaseNoteHandler extends CaseCommandHandler {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.command.add-case-note")
    public void addCaseNote(final Envelope<AddCaseNote> command) throws EventStreamException {

        final AddCaseNote addCaseNote = command.payload();

        applyToCaseAggregate(addCaseNote.getCaseId(), command, caseAggregate -> caseAggregate.addCaseNote(
                addCaseNote.getCaseId(),
                addCaseNote.getNote(),
                addCaseNote.getAuthor(),
                addCaseNote.getDecisionId()));
    }
}
