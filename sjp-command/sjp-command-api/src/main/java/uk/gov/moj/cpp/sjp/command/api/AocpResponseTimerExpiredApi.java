package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.api.service.CaseService;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class AocpResponseTimerExpiredApi {

    @Inject
    private Sender sender;

    @Inject
    private CaseService caseService;

    @Handles("sjp.expire-defendant-aocp-response-timer")
    public void aocpResponseTimerExpiredRequested(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName("sjp.command.controller.expire-defendant-aocp-response-timer")
                                .build(),
                        envelope.payloadAsJsonObject()));
    }
}
