package uk.gov.moj.cpp.sjp.event.processor;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseCompletedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseCompletedProcessor.class);
    private static final String REQUEST_DELETE_DOCS = "sjp.command.request-delete-docs";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Sender sender;

    @Handles(CaseCompleted.EVENT_NAME)
    public void handleCaseCompleted(final JsonEnvelope caseCompletedEnvelope) {
        final JsonObject caseCompletedPayload = caseCompletedEnvelope.payloadAsJsonObject();
        final CaseCompleted caseCompleted = jsonObjectToObjectConverter.convert(caseCompletedPayload, CaseCompleted.class);
        final UUID caseId = caseCompleted.getCaseId();

        requestDeleteDocs(caseCompletedEnvelope, caseId);
    }

    private void requestDeleteDocs(final JsonEnvelope caseCompletedEnvelope, final UUID caseId) {
        LOGGER.info("sending delete docs request for case {}", caseId);
        final JsonObject deleteDocsPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();
        final JsonEnvelope commandEnvelope = envelopeFrom(metadataFrom(caseCompletedEnvelope.metadata()).withName(REQUEST_DELETE_DOCS), deleteDocsPayload);
        sender.send(commandEnvelope);
    }

}
