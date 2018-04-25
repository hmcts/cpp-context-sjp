package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;


@Named
public class WithdrawalRequestedDelegate extends AbstractCaseDelegate {

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution) {
        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName("public.sjp.all-offences-withdrawal-requested")
                .build();

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();

        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));

        execution.setVariable(WITHDRAWAL_REQUESTED_VARIABLE, true);
    }

}