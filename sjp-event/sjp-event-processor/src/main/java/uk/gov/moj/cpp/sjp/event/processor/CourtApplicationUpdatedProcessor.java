package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

/**
 * Subject's address updated while creating application
 * so sending a private message to update defendant details
 */
@ServiceComponent(EVENT_PROCESSOR)
public class CourtApplicationUpdatedProcessor {

    public static final String SJP_COMMAND_UPDATE_DEFENDANT_DETAILS = "sjp.command.update-defendant-details";
    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.events.application-address-changed")
    public void updateDefendantDetailsWithUpdatedAddress(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(SJP_COMMAND_UPDATE_DEFENDANT_DETAILS)
                .withMetadataFrom(envelope));
    }
}
