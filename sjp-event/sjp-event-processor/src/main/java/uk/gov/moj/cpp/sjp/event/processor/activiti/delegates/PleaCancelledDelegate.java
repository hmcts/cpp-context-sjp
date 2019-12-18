package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.PleaUpdatedProcessor.PLEA_CANCELLED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Named
public class PleaCancelledDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleaCancelledDelegate.class);

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        if (!processMigration) {
            final String offenceId = execution.getVariable(OFFENCE_ID_VARIABLE, String.class);

            final Metadata publicEventMetadata = metadataFrom(metadata)
                    .withName(PLEA_CANCELLED_PUBLIC_EVENT_NAME)
                    .build();

            final JsonObject publicEventPayload = Json.createObjectBuilder()
                    .add(CASE_ID, caseId.toString())
                    .add(OFFENCE_ID, offenceId)
                    .build();

            sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
        } else {
            LOGGER.warn("Process migration. Event {} not emitted", PLEA_CANCELLED_PUBLIC_EVENT_NAME);
        }

        execution.removeVariables(asList(PLEA_TYPE_VARIABLE, PLEA_READY_VARIABLE));
    }

}