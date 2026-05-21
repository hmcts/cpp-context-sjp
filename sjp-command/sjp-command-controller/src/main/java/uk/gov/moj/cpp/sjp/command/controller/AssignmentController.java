package uk.gov.moj.cpp.sjp.command.controller;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.ReadyCasesService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_CONTROLLER)
public class AssignmentController {

    @Inject
    private Sender sender;

    @Inject
    private ReadyCasesService readyCasesService;

    @Handles("sjp.command.controller.assign-case")
    public void assignCase(final JsonEnvelope assignCaseCommand) {
        final UUID caseId = fromString(assignCaseCommand.payloadAsJsonObject().getString("caseId"));
        final UUID userId = fromString(assignCaseCommand.payloadAsJsonObject().getString("userId"));

        final JsonObject userReadyCases = readyCasesService.getReadyCasesAssignedToUser(userId, assignCaseCommand);

        final JsonArray casesToBeUnassigned = userReadyCases.getJsonArray("readyCases").getValuesAs(JsonObject.class).stream()
                .map(readyCase -> readyCase.getString("caseId"))
                .filter(readyCaseId -> !readyCaseId.equals(caseId.toString()))
                .reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("userId", userId.toString())
                .add("assignCase", caseId.toString())
                .add("unassignCases", casesToBeUnassigned);

        sender.send(envelop(payload.build()).withName("sjp.command.assign-case").withMetadataFrom(assignCaseCommand));
    }
}
