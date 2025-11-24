package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

/**
 * Command api for marking a case as reopened.
 */
@ServiceComponent(COMMAND_API)
public class CaseReopenedApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.mark-case-reopened-in-libra")
    public void markCaseReopenedInLibra(final JsonEnvelope envelope) {
        handleCaseReopenedCommand(envelope, "sjp.command.mark-case-reopened-in-libra");
    }

    @Handles("sjp.update-case-reopened-in-libra")
    public void updateCaseReopenedInLibra(final JsonEnvelope envelope) {
        handleCaseReopenedCommand(envelope, "sjp.command.update-case-reopened-in-libra");
    }

    @Handles("sjp.undo-case-reopened-in-libra")
    public void undoCaseReopenedInLibra(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.undo-case-reopened-in-libra").apply(envelope.payloadAsJsonObject()));
    }

    private void handleCaseReopenedCommand(final JsonEnvelope envelope, final String commandName) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (!isValidReopenedDate(payload)) {
            throw new BadRequestException("invalid_reopened_date");
        }
        sender.send(enveloper.withMetadataFrom(envelope, commandName).apply(envelope.payloadAsJsonObject()));
    }

    private boolean isValidReopenedDate(final JsonObject jsonObject) {
        final Optional<String> reopenedDate = JsonObjects.getString(jsonObject, "reopenedDate");
        return !(reopenedDate.isPresent() && LocalDates.from(reopenedDate.get()).isAfter(LocalDate.now()));
    }

}
