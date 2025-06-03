package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class UploadCaseDocumentHandler extends CaseCommandHandler {

    @Inject
    private Clock clock;
    @Handles("sjp.command.upload-case-document")
    public void handle(JsonEnvelope command) throws EventStreamException {
        JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = getCaseId(payload);
        final UUID caseDocumentReference = UUID.fromString(payload.getString("caseDocument"));
        final String caseDocumentType = payload.getString("caseDocumentType");
        if(isNull(getUserId(command))){
            command = JsonEnvelope.envelopeFrom(metadataFrom(command.metadata()).withUserId(getUserIdFromCaseAggregate(caseId)),payload);
        }
        applyToCaseAggregate(command,
                aCase -> aCase.uploadCaseDocument(caseId, caseDocumentReference, caseDocumentType)
        );
    }
}
