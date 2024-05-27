package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class TransparencyApi {

    public static final String SJP_COMMAND_REQUEST_TRANSPARENCY_REPORT = "sjp.command.request-transparency-report";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.request-transparency-report")
    public void requestTransparencyReport(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(SJP_COMMAND_REQUEST_TRANSPARENCY_REPORT).build(), envelope.payloadAsJsonObject()));
    }
}
