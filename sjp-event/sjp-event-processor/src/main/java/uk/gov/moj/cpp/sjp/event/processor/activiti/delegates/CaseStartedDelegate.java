package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;

@Named
public class CaseStartedDelegate extends AbstractCaseDelegate {

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution) {
        final String postingDate = execution.getVariable(POSTING_DATE_VARIABLE, String.class);

        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName("public.sjp.sjp-case-created")
                .build();

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add("id", caseId.toString())
                .add("postingDate", postingDate)
                .build();

        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
    }
}
