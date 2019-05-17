package uk.gov.moj.cpp.sjp.command.api;

import static java.lang.String.format;
import static org.apache.commons.lang3.EnumUtils.getEnum;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.ADJOURNMENT;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.BadRequestException;

@ServiceComponent(COMMAND_API)
public class CaseNoteApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.add-case-note")
    public void addCaseNote(final JsonEnvelope addCaseNoteCommand) {
        final JsonObject addNote = addCaseNoteCommand.payloadAsJsonObject();

        final NoteType noteType = getEnum(NoteType.class, addNote.getString("noteType"));
        final boolean hasDecisionId = addNote.containsKey("decisionId");

        if ((noteType == DECISION || noteType == ADJOURNMENT) && !hasDecisionId) {
            throw new BadRequestException(format("Field decisionId is required for %s note", noteType));
        }

        if ((noteType != NoteType.DECISION && noteType != ADJOURNMENT) && hasDecisionId) {
            throw new BadRequestException(format("Field decisionId is not allowed for %s note", noteType));
        }

        sender.send(enveloper.withMetadataFrom(addCaseNoteCommand, "sjp.command.controller.add-case-note")
                .apply(addCaseNoteCommand.payloadAsJsonObject()));
    }
}
