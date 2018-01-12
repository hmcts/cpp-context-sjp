package uk.gov.moj.cpp.sjp.event.processor.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.DESCRIPTION;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseUpdatedListener {

    static final String CASE_REOPENED_IN_LIBRA_PUBLIC_EVENT = "public.structure.case-reopened-in-libra";
    static final String CASE_REOPENED_IN_LIBRA_UPDATED_PUBLIC_EVENT = "public.structure.case-reopened-in-libra-updated";
    static final String CASE_REOPENED_IN_LIBRA_UNDONE_PUBLIC_EVENT = "public.structure.case-reopened-in-libra-undone";

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseUpdatedListener.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.case-reopened-in-libra")
    public void handleCaseReopenedInLibra(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Marked case reopened in libra for caseId: " + payload.getString(CASE_ID));
        }
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, CASE_REOPENED_IN_LIBRA_PUBLIC_EVENT).apply(payload));
    }

    @Handles("sjp.events.case-reopened-in-libra-updated")
    public void handleCaseReopenedInLibraUpdated(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Updated reopened case in libra for caseId: " + payload.getString(CASE_ID));
        }
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, CASE_REOPENED_IN_LIBRA_UPDATED_PUBLIC_EVENT).apply(payload));
    }

    @Handles("sjp.events.case-reopened-in-libra-undone")
    public void handleCaseReopenedInLibraUndone(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Undone reopened case in libra for caseId: " + payload.getString(CASE_ID));
        }
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, CASE_REOPENED_IN_LIBRA_UNDONE_PUBLIC_EVENT).apply(payload));
    }
}
