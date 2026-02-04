package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA;
import static uk.gov.moj.cpp.sjp.event.processor.PleaUpdatedProcessor.PLEA_UPDATED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Named
public class PleaUpdatedDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleaUpdatedDelegate.class);

    @Override
    @SuppressWarnings("squid:S4274")
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        assert execution.hasVariable(PLEA_TYPE_VARIABLE) : PLEA_TYPE_VARIABLE + " must be present";

        final String offenceId = execution.getVariable(OFFENCE_ID_VARIABLE, String.class);
        final PleaType pleaType = PleaType.valueOf(execution.getVariable(PLEA_TYPE_VARIABLE, String.class));

        if (!processMigration) {
            final Metadata publicEventMetadata = metadataFrom(metadata)
                    .withName(PLEA_UPDATED_PUBLIC_EVENT_NAME)
                    .build();

            final JsonObject publicEventPayload = createObjectBuilder()
                    .add(CASE_ID, caseId.toString())
                    .add(OFFENCE_ID, offenceId)
                    .add(PLEA, pleaType.name())
                    .build();

            sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
        } else {
            LOGGER.warn("Process migration. Event {} not emitted", PLEA_UPDATED_PUBLIC_EVENT_NAME);
        }

        final boolean areDatesToAvoidSet = execution.hasVariable(DATES_TO_AVOID_VARIABLE);

        execution.setVariable(PLEA_READY_VARIABLE, isPleaReady(pleaType, areDatesToAvoidSet));
    }

    /**
     * WARNING: if you change this condition update the same into the activity at flowNonGuilty
     */
    public static boolean isPleaReady(final PleaType pleaType, final boolean areDatesToAvoidSet) {
        return !PleaType.NOT_GUILTY.equals(pleaType) || areDatesToAvoidSet;
    }

}