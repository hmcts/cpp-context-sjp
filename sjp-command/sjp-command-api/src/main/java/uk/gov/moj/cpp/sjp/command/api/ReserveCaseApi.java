package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;


import javax.inject.Inject;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(COMMAND_API)
public class ReserveCaseApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.reserve-case")
    public void reserveCaseStatus(final JsonEnvelope reserveCaseStatusCommand) {

        sender.send(envelopeFrom(
                metadataFrom(
                        reserveCaseStatusCommand.metadata()).withName("sjp.command.reserve-case"),
                reserveCaseStatusCommand.payloadAsJsonObject()));
    }

    @Handles("sjp.undo-reserve-case")
    public void undoReserveCaseStatus(final JsonEnvelope undoReserveCaseStatusCommand) {

        sender.send(envelopeFrom(
                metadataFrom(
                        undoReserveCaseStatusCommand.metadata()).withName("sjp.command.undo-reserve-case"),
                undoReserveCaseStatusCommand.payloadAsJsonObject()));
    }
}
