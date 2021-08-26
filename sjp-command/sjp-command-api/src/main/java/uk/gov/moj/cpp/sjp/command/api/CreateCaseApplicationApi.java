package uk.gov.moj.cpp.sjp.command.api;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(COMMAND_API)
public class CreateCaseApplicationApi {

    @Inject
    private Sender sender;

    @Handles("sjp.create-case-application")
    public void createCaseApplication(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.command.controller.create-case-application").build(),
                envelope.payloadAsJsonObject()
        ));
    }

}
