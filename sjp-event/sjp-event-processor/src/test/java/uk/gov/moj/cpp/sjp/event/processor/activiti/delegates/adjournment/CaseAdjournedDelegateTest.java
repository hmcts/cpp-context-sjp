package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.adjournment;

import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;

import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.AbstractCaseDelegateTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAdjournedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private CaseAdjournedDelegate caseAdjournedDelegate;

    @Test
    public void shouldSetCaseAdjournmentVariable() {
        caseAdjournedDelegate.execute(delegateExecution);

        verify(delegateExecution).setVariable(CASE_ADJOURNED_VARIABLE, true);
    }

}
