package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class AddCaseDocumentHandler extends CaseCommandHandler {

    @Inject
    private Clock clock;

    @Handles("sjp.command.add-case-document")
    public void addCaseDocument(final JsonEnvelope command) throws EventStreamException {
        JsonObject payload = command.payloadAsJsonObject();

        final CaseDocument caseDocument = caseDocumentFrom(payload);

        applyToCaseAggregate(command,
                aCase -> aCase.addCaseDocument(getCaseId(command.payloadAsJsonObject()),
                        caseDocument)
        );
    }

    private CaseDocument caseDocumentFrom(final JsonObject payload) {
        return new CaseDocument(
                UUID.fromString(payload.getString("id")),
                UUID.fromString(payload.getString("materialId")),
                payload.getString("documentType", null),
                clock.now());
    }

}
