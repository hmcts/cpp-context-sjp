package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.POSTING_DATE;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseReceivedProcessor {

    @Inject
    private CaseStateService caseStateService;

    @Handles(CaseReceived.EVENT_NAME)
    public void handleCaseReceivedEvent(final JsonEnvelope event) {
        final UUID caseId = UUID.fromString(event.payloadAsJsonObject().getString(CASE_ID));
        final LocalDate postingDate = LocalDate.parse(event.payloadAsJsonObject().getString(POSTING_DATE));

        caseStateService.caseReceived(caseId, postingDate, event.metadata());
    }
}
