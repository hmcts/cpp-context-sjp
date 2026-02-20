package uk.gov.moj.cpp.sjp.event.processor;


import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.APPLICATION_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.APPLICATION_REFERENCE;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementNotificationService;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseApplicationRecordedProcessor {

    static final String CASE_APPLICATION_RECORDED_PUBLIC_EVENT = "public.sjp.case-application-recorded";


    private static final Logger LOGGER = LoggerFactory.getLogger(CaseApplicationRecordedProcessor.class.getCanonicalName());

    @Inject
    private EnforcementNotificationService enforcementNotificationService;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("sjp.events.case-application-recorded")
    public void handleCaseApplicationRecorded(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final CaseApplicationRecorded caseApplicationRecorded = jsonObjectToObjectConverter.convert(payload, CaseApplicationRecorded.class);
        if(nonNull(caseApplicationRecorded.getCourtApplication()) && nonNull(caseApplicationRecorded.getCourtApplication().getApplicationReference())) {
            final String applicationId = caseApplicationRecorded.getCourtApplication().getId().toString();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stat dec application recorded for applicationId : {}", applicationId);
            }

            final JsonObject jsonObject = Json.createObjectBuilder()
                    .add(APPLICATION_ID, applicationId)
                    .add(APPLICATION_REFERENCE, caseApplicationRecorded.getCourtApplication().getApplicationReference()).build();
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, CASE_APPLICATION_RECORDED_PUBLIC_EVENT).apply(jsonObject));
            enforcementNotificationService.checkIfEnforcementToBeNotified(caseApplicationRecorded.getCaseId(), jsonEnvelope);
        } else {
            LOGGER.error("Stat dec application sjp.events.case-application-recorded, CourtApplication not found");
        }
    }
}
