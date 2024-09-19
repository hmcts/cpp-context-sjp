package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.json.schemas.domains.sjp.command.AddCaseAssignmentRestriction;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class CaseAssignmentRestrictionApi {

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("sjp.add-case-assignment-restriction")
    public void addCaseAssignmentRestriction(final Envelope<AddCaseAssignmentRestriction> envelope) {
        final AddCaseAssignmentRestriction addCaseAssignmentRestriction = envelope.payload();
        final JsonObject payload = objectToJsonObjectConverter.convert(addCaseAssignmentRestriction);

        sender.send(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.command.controller.add-case-assignment-restriction").build(),
                payload));
    }
}
