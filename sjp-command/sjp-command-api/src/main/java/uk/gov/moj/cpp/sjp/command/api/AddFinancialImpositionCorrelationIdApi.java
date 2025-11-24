package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class AddFinancialImpositionCorrelationIdApi {

    @Inject
    private Sender sender;

    private static final String COMMAND_CONTROLLER_NAME = "sjp.command.add-financial-imposition-correlation-id";

    @Handles("sjp.add-financial-imposition-correlation-id")
    public void addFinancialImpositionCorrelationId(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(
                metadataFrom(
                    envelope.metadata()).withName(COMMAND_CONTROLLER_NAME),
                envelope.payloadAsJsonObject())
        );
    }

}
