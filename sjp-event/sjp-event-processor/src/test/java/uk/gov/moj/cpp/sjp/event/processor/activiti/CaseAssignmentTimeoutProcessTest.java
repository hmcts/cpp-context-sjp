package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.ASSIGNMENT_TIMED_OUT;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.DelegateExecutionProcessBusinessKeyMatcher.withProcessBusinessKey;

import java.time.Duration;
import java.util.UUID;

import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CaseAssignmentTimeoutProcessTest {

    private static final String TIMEOUT_PROCESS_PATH = "processes/caseAssignmentTimeout.bpmn20.xml";
    private static final int MAX_MILLIS_TO_WAIT = 10000;

    @Rule
    public ActivitiRule rule = new ActivitiRule();

    private CaseAssignmentTimeoutProcess caseAssignmentTimeoutProcess;

    private UUID caseId;

    private DelegatesVerifier delegatesVerifier;

    @Before
    public void init() {

        caseId = randomUUID();

        caseAssignmentTimeoutProcess = new CaseAssignmentTimeoutProcess(rule.getRuntimeService());

        delegatesVerifier = new DelegatesVerifier(rule);
    }

    @Test
    @Deployment(resources = TIMEOUT_PROCESS_PATH)
    public void shouldTriggerTimeoutTaskAfterTimeout() {

        final ProcessInstance processInstance = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(2));

        delegatesVerifier.tryProcessPendingJobs();

        delegatesVerifier.verifyDelegatesInteractionWith(ASSIGNMENT_TIMED_OUT);
        delegatesVerifier.verifyNumberOfExecution(ASSIGNMENT_TIMED_OUT, 1);
        delegatesVerifier.assertDelegateCalledWith(ASSIGNMENT_TIMED_OUT, 0, withProcessBusinessKey(caseId.toString()));
        delegatesVerifier.assertProcessFinished(processInstance.getId(), true);
    }

    @Test
    @Deployment(resources = TIMEOUT_PROCESS_PATH)
    public void shouldCancelAssignmentTimer() {

        final ProcessInstance processInstance = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));
        caseAssignmentTimeoutProcess.cancelTimer(caseId);

        delegatesVerifier.verifyNumberOfExecution(ASSIGNMENT_TIMED_OUT, 0);
        delegatesVerifier.verifyDelegatesInteractionWith();
        delegatesVerifier.assertProcessFinished(processInstance.getId(), true);
    }

    @Test
    @Deployment(resources = TIMEOUT_PROCESS_PATH)
    public void shouldFailGracefullyCancellingNonExistentAssignmentTimer() {

        caseAssignmentTimeoutProcess.cancelTimer(caseId);
    }

    @Test
    @Deployment(resources = TIMEOUT_PROCESS_PATH)
    public void shouldNotThrowAnExceptionForDuplicateTimers() {

        final ProcessInstance processInstance1 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));
        final ProcessInstance processInstance2 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));

        delegatesVerifier.tryProcessPendingJobs();

        delegatesVerifier.verifyNumberOfExecution(ASSIGNMENT_TIMED_OUT, 2);
        delegatesVerifier.assertProcessFinished(processInstance1.getId(), true);
        delegatesVerifier.assertProcessFinished(processInstance2.getId(), true);
    }

    @Test
    @Deployment(resources = TIMEOUT_PROCESS_PATH)
    public void shouldCancelDuplicateTimers() {

        final ProcessInstance processInstance1 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));
        final ProcessInstance processInstance2 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));

        caseAssignmentTimeoutProcess.cancelTimer(caseId);

        delegatesVerifier.assertProcessFinished(processInstance1.getId(), true);
        delegatesVerifier.assertProcessFinished(processInstance2.getId(), true);
    }

    @Test
    @Deployment(resources = TIMEOUT_PROCESS_PATH)
    public void shouldResetTimer() {

        ProcessInstance processInstance1 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(MAX_MILLIS_TO_WAIT * 2));

        ProcessInstance processInstance2 = caseAssignmentTimeoutProcess.resetTimer(caseId, Duration.ofSeconds(1));

        delegatesVerifier.tryProcessPendingJobs();

        delegatesVerifier.assertProcessFinished(processInstance1.getId(), true);
        delegatesVerifier.assertProcessFinished(processInstance2.getId(), true);
    }
}
