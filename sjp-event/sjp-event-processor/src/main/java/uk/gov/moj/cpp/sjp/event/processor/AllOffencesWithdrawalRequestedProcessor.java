package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.inject.Inject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class AllOffencesWithdrawalRequestedProcessor {

    @Inject
    private CaseStateService caseStateService;

    @Handles(AllOffencesWithdrawalRequested.EVENT_NAME)
    public void handleAllOffencesWithdrawalEvent(final JsonEnvelope event) {
        final UUID caseId = UUID.fromString(event.payloadAsJsonObject().getString(CASE_ID));
        caseStateService.withdrawalRequested(caseId, event.metadata());
    }
}
