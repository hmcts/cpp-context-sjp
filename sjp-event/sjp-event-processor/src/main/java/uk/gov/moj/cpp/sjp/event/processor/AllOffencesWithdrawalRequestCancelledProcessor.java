package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.inject.Inject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class AllOffencesWithdrawalRequestCancelledProcessor {

    public static final String WITHDRAWAL_REQUEST_CANCELLED_PUBLIC_EVENT_NAME = "public.sjp.all-offences-withdrawal-request-cancelled";

    @Inject
    private CaseStateService caseStateService;

    @Inject
    private Sender sender;

    @Handles(AllOffencesWithdrawalRequestCancelled.EVENT_NAME)
    public void handleWithdrawalRequestCancellation(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(WITHDRAWAL_REQUEST_CANCELLED_PUBLIC_EVENT_NAME)
                .withMetadataFrom(envelope));

        updateActivityForExistingCasesUsingTheLegacyFlow(envelope);
    }

    private void updateActivityForExistingCasesUsingTheLegacyFlow(final JsonEnvelope envelope) {
        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        caseStateService.withdrawalRequestCancelled(caseId, envelope.metadata());
    }
}
