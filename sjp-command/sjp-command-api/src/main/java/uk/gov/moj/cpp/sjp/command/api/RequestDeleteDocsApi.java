package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class RequestDeleteDocsApi {

    @Inject
    private Sender sender;

    @Handles("sjp.request-delete-docs")
    public void requestDeleteDocs(final JsonEnvelope requestDeleteDocsCommand) {
        sender.send(JsonEnvelope.envelopeFrom(
                metadataFrom(requestDeleteDocsCommand.metadata())
                        .withName("sjp.command.request-delete-docs"),
                requestDeleteDocsCommand.payloadAsJsonObject()
        ));
    }
}
