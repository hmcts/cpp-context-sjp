package uk.gov.moj.cpp.sjp.command.controller;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.CaseApplicationService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

@ServiceComponent(COMMAND_CONTROLLER)
public class CreateApplicationController {

    @Inject
    private Sender sender;

    @Inject
    private CaseApplicationService caseApplicationService;

    @Handles("sjp.command.controller.create-case-application")
    public void createCaseApplication(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObject applicationDetail = caseApplicationService.getApplicationDetails(envelope);
        final JsonObjectBuilder enrichedPayload = createObjectBuilder(payload)
                .add("applicationIdExists", !applicationDetail.isEmpty());

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.command.handler.create-case-application"),
                enrichedPayload.build())
        );
    }

}