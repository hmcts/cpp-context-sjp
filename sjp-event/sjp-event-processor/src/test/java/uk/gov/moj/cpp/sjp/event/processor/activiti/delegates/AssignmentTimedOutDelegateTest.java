package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentService;

import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentTimedOutDelegateTest {

    @Mock
    protected DelegateExecution delegateExecution;
    @InjectMocks
    private AssignmentTimedOutDelegate assignmentTimedOutDelegate;
    @Mock
    private AssignmentService assignmentService;

    @Test
    public void shouldUnassignCase() {

        final UUID caseId = UUID.randomUUID();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());

        assignmentTimedOutDelegate.execute(delegateExecution);

        verify(assignmentService).unassignCase(caseId);
    }
}