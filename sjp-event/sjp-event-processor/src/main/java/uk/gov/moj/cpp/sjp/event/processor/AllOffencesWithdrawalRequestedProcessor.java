package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.inject.Inject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class AllOffencesWithdrawalRequestedProcessor {

    public static final String WITHDRAWAL_REQUESTED_PUBLIC_EVENT_NAME = "public.sjp.all-offences-withdrawal-requested";

    @Inject
    private CaseStateService caseStateService;

    @Inject
    private Sender sender;

    @Handles(AllOffencesWithdrawalRequested.EVENT_NAME)
    public void handleAllOffencesWithdrawalEvent(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(WITHDRAWAL_REQUESTED_PUBLIC_EVENT_NAME)
                .withMetadataFrom(envelope));

        updateActivityForExistingCasesUsingTheLegacyFlow(envelope);
    }

    private void updateActivityForExistingCasesUsingTheLegacyFlow(final JsonEnvelope envelope) {
        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        caseStateService.withdrawalRequested(caseId, envelope.metadata());
    }
}
