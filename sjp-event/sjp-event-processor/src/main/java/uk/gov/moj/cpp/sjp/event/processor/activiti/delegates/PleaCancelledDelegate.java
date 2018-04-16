package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;


@Named
public class PleaCancelledDelegate extends AbstractCaseDelegate {

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution) {
        final String offenceId = execution.getVariable(OFFENCE_ID_VARIABLE, String.class);

        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName("public.sjp.plea-cancelled")
                .build();

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(OFFENCE_ID, offenceId)
                .build();

        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));

        execution.removeVariable(PLEA_TYPE_VARIABLE);
    }

}