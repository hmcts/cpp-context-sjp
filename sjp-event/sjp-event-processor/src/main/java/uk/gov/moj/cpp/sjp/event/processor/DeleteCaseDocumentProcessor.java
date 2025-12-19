package uk.gov.moj.cpp.sjp.event.processor;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class DeleteCaseDocumentProcessor {

    public static final String MATERIAL_COMMAND_DELETE_MATERIAL = "material.command.delete-material";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.case-document-deleted")
    public void processCaseDocumentDeleted(final JsonEnvelope envelope) {

        final String caseId = envelope.payloadAsJsonObject().getString("caseId");
        final String materialId = envelope.payloadAsJsonObject().getJsonObject("caseDocument").getString("materialId");
        final String documentId = envelope.payloadAsJsonObject().getJsonObject("caseDocument").getString("id");

        sender.send(Enveloper.envelop(createObjectBuilder()
                        .add("materialId", materialId)
                        .build())
                .withName(MATERIAL_COMMAND_DELETE_MATERIAL)
                .withMetadataFrom(envelope));

        sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                .withName("public.sjp.delete-case-document-request-accepted").build(), createObjectBuilder()
                .add("caseId", caseId)
                .add("documentId", documentId)
                .build()));
    }

    @Handles("sjp.events.delete-case-document-request-rejected")
    public void deleteCaseDocumentRejected(final JsonEnvelope envelope) {

        sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                .withName("public.sjp.delete-case-document-request-rejected").build(), envelope.payloadAsJsonObject()));
    }

}
