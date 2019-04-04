package uk.gov.moj.cpp.sjp.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseCompletedProcessor {

    @Inject
    private CaseStateService caseStateService;

    @Handles(CaseCompleted.EVENT_NAME)
    public void handleCaseCompleted(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"));
        caseStateService.caseCompleted(caseId, envelope.metadata());
    }
}
