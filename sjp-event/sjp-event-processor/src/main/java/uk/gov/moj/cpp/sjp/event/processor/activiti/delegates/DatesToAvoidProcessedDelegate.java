package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.DatesToAvoidProcessor.DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;

/**
 * This delegate is made to run just once. To verify this we ensure that the DATES_TO_AVOID_VARIABLE
 * is set and verify the presence of this variable elsewhere.
 */
@Named
public class DatesToAvoidProcessedDelegate extends AbstractCaseDelegate {

    @Override
    @SuppressWarnings("squid:S4274")
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        assert PleaType.NOT_GUILTY.name().equals(execution.getVariable(PLEA_TYPE_VARIABLE, String.class))
                : "dates to avoid are available just for PLEA_NOT_GUILTY";

        if (execution.hasVariable(DATES_TO_AVOID_VARIABLE)) {
            send(metadata, DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME,
                    createObjectBuilder()
                            .add(CASE_ID, caseId.toString())
                            .add(DATES_TO_AVOID, execution.getVariable(DATES_TO_AVOID_VARIABLE, String.class))
                            .build());
        } else {
            // this will prevent possibility of wait for dates-to-avoid ever again - one possibility to add them for the prosecutor.
            execution.setVariable(DATES_TO_AVOID_VARIABLE, "DATES-TO-AVOID not submitted after 10 days.");
        }

        execution.setVariable(PLEA_READY_VARIABLE, true);
    }

}