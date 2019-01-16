package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.adjournment;

import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.AbstractCaseDelegate;

import java.util.UUID;

import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;

@Named
public class CaseAdjournedDelegate extends AbstractCaseDelegate {

    @Override
    public void execute(final UUID caseId,
                        final Metadata metadata,
                        final DelegateExecution execution,
                        final boolean processMigration) {

        execution.setVariable(CASE_ADJOURNED_VARIABLE, true);
    }
}
