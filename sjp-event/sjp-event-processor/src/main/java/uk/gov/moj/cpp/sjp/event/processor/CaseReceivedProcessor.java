package uk.gov.moj.cpp.sjp.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.EXPECTED_DATE_READY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.POSTING_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.URN;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.processor.service.AzureFunctionService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseReceivedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReceivedProcessor.class);
    public static final String CASE_STARTED_PUBLIC_EVENT_NAME = "public.sjp.sjp-case-created";

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    protected Sender sender;

    @Inject
    private TimerService timerService;

    @Inject
    private AzureFunctionService azureFunctionService;


    @Handles(CaseReceived.EVENT_NAME)
    public void handleCaseReceivedEvent(final JsonEnvelope event) {
        final UUID caseId = UUID.fromString(event.payloadAsJsonObject().getString(CASE_ID));
        final String caseUrn = event.payloadAsJsonObject().getString(URN);
        final LocalDate postingDate = LocalDate.parse(event.payloadAsJsonObject().getString(POSTING_DATE));
        final LocalDate expectedDateReady = LocalDate.parse(event.payloadAsJsonObject().getString(EXPECTED_DATE_READY));
        raisePublicEvent(event.metadata(), caseId, postingDate);
        relayCaseToCourtStore(caseUrn);
        //Does activiti and JMS share the same transaction?
        timerService.startTimerForDefendantResponse(caseId, expectedDateReady, event.metadata());
    }

    private void raisePublicEvent(final Metadata metadata, final UUID caseId, final LocalDate postingDate) {
        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName(CASE_STARTED_PUBLIC_EVENT_NAME)
                .build();

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add("id", caseId.toString())
                .add("postingDate", postingDate.toString())
                .build();
        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
    }

    private void relayCaseToCourtStore(String caseId) {

        if (!caseId.isEmpty()) {
            final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
            payloadBuilder.add("CaseReference", caseId);
            try {
                this.azureFunctionService.relayCaseOnCPP(payloadBuilder.build().toString());
            } catch (IOException ex) {
                LOGGER.error("Error relaying case to court store.",ex);
           }
        }
    }
}