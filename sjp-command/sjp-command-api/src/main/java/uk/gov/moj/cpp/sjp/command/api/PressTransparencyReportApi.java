package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class PressTransparencyReportApi {

    @Inject
    private Sender sender;

    @Handles("sjp.request-press-transparency-report")
    public void requestTransparencyReport(final JsonEnvelope requestPressTransparencyCommand) {
        sender.send(envelopeFrom(
                metadataFrom(requestPressTransparencyCommand.metadata())
                        .withName("sjp.command.request-press-transparency-report").build(),
                requestPressTransparencyCommand.payloadAsJsonObject()
                )
        );
    }
}
