package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

/**
 * This command is only used for ATCM-6980 BDF and can be deprecated once ran however the event
 * handlers of the private events created from invoking this command need to remain for rebuild and
 * catchup
 */
@ServiceComponent(COMMAND_API)
public class AddFinancialImpositionAccountNumberBdfApi {

    @Inject
    private Sender sender;

    private static final String COMMAND_CONTROLLER_NAME = "sjp.command.add-financial-imposition-account-number-bdf";

    @Handles("sjp.add-financial-imposition-account-number-bdf")
    public void addFinancialImpositionAccountNumber(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(
                metadataFrom(
                        envelope.metadata()).withName(COMMAND_CONTROLLER_NAME),
                envelope.payloadAsJsonObject())
        );
    }

}
