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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Named
public class CaseCompletedDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseCompletedDelegate.class);
    private static final String CASE_COMPLETED_COMMAND_NAME = "sjp.command.complete-case";

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        if (!processMigration) {
            final Metadata commandMetadata = metadataFrom(metadata)
                    .withName(CASE_COMPLETED_COMMAND_NAME)
                    .build();

            final JsonObject commandPayload = createObjectBuilder()
                    .add(CASE_ID, caseId.toString())
                    .build();

            sender.sendAsAdmin(envelopeFrom(commandMetadata, commandPayload));
        } else {
            LOGGER.warn("Process migration. Command {} not sent", CASE_COMPLETED_COMMAND_NAME);
        }
    }

}