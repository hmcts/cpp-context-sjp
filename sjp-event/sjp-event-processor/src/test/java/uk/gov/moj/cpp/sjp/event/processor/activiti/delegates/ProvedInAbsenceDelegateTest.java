package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProvedInAbsenceDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private ProvedInAbsenceDelegate provedInAbsenceDelegate;

    @Test
    public void shouldSetProcessVariables() {
        provedInAbsenceDelegate.execute(delegateExecution);

        verify(delegateExecution).setVariable(PROVED_IN_ABSENCE_VARIABLE, true);
    }
}
