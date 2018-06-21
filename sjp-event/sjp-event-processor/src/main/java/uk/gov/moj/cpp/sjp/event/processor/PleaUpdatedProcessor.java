package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class PleaUpdatedProcessor {

    @Inject
    private CaseStateService caseStateService;

    @Handles(PleaUpdated.EVENT_NAME)
    public void handlePleaUpdated(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        final UUID offenceId = UUID.fromString(envelope.payloadAsJsonObject().getString(OFFENCE_ID));
        final PleaType plea = PleaType.valueOf(envelope.payloadAsJsonObject().getString(PLEA));

        caseStateService.pleaUpdated(caseId, offenceId, plea, envelope.metadata());
    }

    @Handles(PleaCancelled.EVENT_NAME)
    public void handlePleaCancelled(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        final UUID offenceId = UUID.fromString(envelope.payloadAsJsonObject().getString(OFFENCE_ID));

        caseStateService.pleaCancelled(caseId, offenceId, envelope.metadata());
    }

}
