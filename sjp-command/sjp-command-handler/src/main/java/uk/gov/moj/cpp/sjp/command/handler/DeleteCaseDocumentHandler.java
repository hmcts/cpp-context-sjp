package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class DeleteCaseDocumentHandler extends CaseCommandHandler {

    @Handles("sjp.command.delete-case-document")
    public void deleteCaseDocument(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID documentId = UUID.fromString(payload.getString("documentId"));

        applyToCaseAggregate(command, aCase -> aCase.deleteCaseDocument(documentId));
    }
}
