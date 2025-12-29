package uk.gov.moj.cpp.sjp.command.controller;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.UserService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_CONTROLLER)
public class CaseNoteController {

    @Inject
    private Clock clock;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private UserService userService;

    @Handles("sjp.command.controller.add-case-note")
    public void addCaseNote(final JsonEnvelope addCaseNoteCommand) {
        final JsonObject addCaseNote = addCaseNoteCommand.payloadAsJsonObject();

        final JsonObject userDetails = userService.getCallingUserDetails(addCaseNoteCommand);
        final String callerId = addCaseNoteCommand.metadata().userId().orElseThrow(() ->
                new IllegalStateException(format("Envelope with id %s does not contains user id", addCaseNoteCommand.metadata().id())));

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", addCaseNote.getString("caseId"))
                .add("note", createObjectBuilder()
                        .add("text", addCaseNote.getString("noteText"))
                        .add("type", addCaseNote.getString("noteType"))
                        .add("id", randomUUID().toString())
                        .add("addedAt", clock.now().toString()))
                .add("author", createObjectBuilder()
                        .add("userId", callerId)
                        .add("firstName", userDetails.getString("firstName"))
                        .add("lastName", userDetails.getString("lastName")));


        ofNullable(addCaseNote.getString("decisionId", null))
                .ifPresent(decisionId -> payload.add("decisionId", decisionId));

        sender.send(enveloper.withMetadataFrom(addCaseNoteCommand, "sjp.command.add-case-note").apply(payload.build()));
    }
}
