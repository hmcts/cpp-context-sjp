package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.AllOffencesWithdrawalRequestedProcessor.WITHDRAWAL_REQUESTED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Named
public class WithdrawalRequestedDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(WithdrawalRequestedDelegate.class);

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        if (!processMigration) {
            final Metadata publicEventMetadata = metadataFrom(metadata)
                    .withName(WITHDRAWAL_REQUESTED_PUBLIC_EVENT_NAME)
                    .build();

            final JsonObject publicEventPayload = JsonObjects.createObjectBuilder()
                    .add(CASE_ID, caseId.toString())
                    .build();

            sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
        } else {
            LOGGER.warn("Process migration. Event {} not emitted", WITHDRAWAL_REQUESTED_PUBLIC_EVENT_NAME);
        }
        execution.setVariable(WITHDRAWAL_REQUESTED_VARIABLE, true);
    }

}