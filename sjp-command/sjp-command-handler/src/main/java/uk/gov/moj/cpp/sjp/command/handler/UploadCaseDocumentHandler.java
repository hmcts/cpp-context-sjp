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

    static final String CASE_DOCUMENT = "caseDocument";
    static final String CASE_DOCUMENT_URI = "caseDocumentUri";

    @Inject
    private Clock clock;
    @Handles("sjp.command.upload-case-document")
    public void handle(JsonEnvelope command) throws EventStreamException {
        JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = getCaseId(payload);
        final String caseDocumentType = payload.getString("caseDocumentType");

        // At least one of a file service id (caseDocument) or a blob uri (caseDocumentUri) is
        // present - the command schema's anyOf enforces that - and both may arrive together, which
        // is what staging-dvla sends. Pass through whatever came; the aggregate and the processor
        // decide what each of them means.
        final String caseDocumentReference = valueOrNull(payload, CASE_DOCUMENT);
        final String caseDocumentUri = valueOrNull(payload, CASE_DOCUMENT_URI);

        final UUID fileServiceId = caseDocumentReference == null ? null : UUID.fromString(caseDocumentReference);

        if(isNull(getUserId(command))){
            command = JsonEnvelope.envelopeFrom(metadataFrom(command.metadata()).withUserId(getUserIdFromCaseAggregate(caseId)),payload);
        }
        applyToCaseAggregate(command,
                aCase -> aCase.uploadCaseDocument(caseId, fileServiceId, caseDocumentUri, caseDocumentType)
        );
    }

    private static String valueOrNull(final JsonObject payload, final String field) {
        return payload.containsKey(field) && !payload.isNull(field) ? payload.getString(field) : null;
    }
}
