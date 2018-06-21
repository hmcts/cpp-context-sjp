package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;

import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;


@Named
public class ProvedInAbsenceDelegate implements JavaDelegate {

    @Override
    public void execute(final DelegateExecution execution) {
        execution.setVariable(PROVED_IN_ABSENCE_VARIABLE, true);
    }

}