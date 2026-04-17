package uk.gov.moj.cpp.sjp.command.controller;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.json.schemas.domains.sjp.command.AddCaseAssignmentRestriction;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_CONTROLLER)
public class CaseAssignmentRestrictionController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.controller.add-case-assignment-restriction")
    public void addCaseAssignmentRestriction(final Envelope<AddCaseAssignmentRestriction> command) {
        final AddCaseAssignmentRestriction payload = command.payload();
        final JsonObjectBuilder commandPayload = createObjectBuilder()
                .add("prosecutingAuthority", payload.getProsecutingAuthority())
                .add("includeOnly", getRestrictions(payload.getIncludeOnly()))
                .add("exclude", getRestrictions(payload.getExclude()));

        sender.send(envelop(commandPayload.build())
                .withName("sjp.command.add-case-assignment-restriction")
                .withMetadataFrom(command));
    }

    private JsonArrayBuilder getRestrictions(final List<String> restrictions) {
        final JsonArrayBuilder newRestrictions = createArrayBuilder();
        restrictions.forEach(newRestrictions::add);
        return newRestrictions;
    }
}
