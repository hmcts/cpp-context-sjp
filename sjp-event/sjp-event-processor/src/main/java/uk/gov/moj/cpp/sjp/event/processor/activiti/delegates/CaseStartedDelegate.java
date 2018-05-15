package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CREATE_BY_PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class CaseStartedDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseStartedDelegate.class);
    private static final String CASE_STARTED_PUBLIC_EVENT_NAME = "public.sjp.sjp-case-created";

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {

        if (!processMigration) {
            final String postingDate = execution.getVariable(POSTING_DATE_VARIABLE, String.class);

            final Metadata publicEventMetadata = metadataFrom(metadata)
                    .withName(CASE_STARTED_PUBLIC_EVENT_NAME)
                    .build();

            final JsonObject publicEventPayload = Json.createObjectBuilder()
                    .add("id", caseId.toString())
                    .add("postingDate", postingDate)
                    .build();

            sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
        } else {
            LOGGER.warn("Process migration. Event {} not emitted", CASE_STARTED_PUBLIC_EVENT_NAME);
            execution.setVariable(CREATE_BY_PROCESS_MIGRATION_VARIABLE, true);
        }
    }
}