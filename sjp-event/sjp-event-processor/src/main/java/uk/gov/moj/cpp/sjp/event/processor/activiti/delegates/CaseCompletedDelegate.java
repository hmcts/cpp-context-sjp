package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;


@Named
public class CaseCompletedDelegate extends AbstractCaseDelegate {

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution) {
        final Metadata commandMetadata = metadataFrom(metadata)
                .withName("sjp.command.complete-case")
                .build();

        final JsonObject commandPayload = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();

        sender.sendAsAdmin(envelopeFrom(commandMetadata, commandPayload));
    }

}