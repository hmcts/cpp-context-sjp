package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.util.UUID.randomUUID;
import static org.activiti.engine.impl.test.JobTestHelper.waitForJobExecutorToProcessAllJobs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.DelegateExecutionBusinessKeyMatcher.withBusinessKey;

import java.time.Duration;
import java.util.UUID;

import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAssignmentTimeoutProcessTest {

    private static final String TIMEOUT_PROCESS_PATH = "processes/caseAssignmentTimeout.bpmn20.xml";
    private static final int MAX_MILLIS_TO_WAIT = 10000;

    @Rule
    public ActivitiRule rule = new ActivitiRule();

    @InjectMocks
    private CaseAssignmentTimeoutProcess caseAssignmentTimeoutProcess;

    private JavaDelegate assignmentTimedOutDelegate;
    private UUID caseId;

    @Before
    public void init() {

        caseId = randomUUID();

        caseAssignmentTimeoutProcess = new CaseAssignmentTimeoutProcess(rule.getRuntimeService());

        final StandaloneProcessEngineConfiguration configuration = (StandaloneProcessEngineConfiguration) rule.getProcessEngine().getProcessEngineConfiguration();

        assignmentTimedOutDelegate = (JavaDelegate) configuration.getBeans().get("assignmentTimedOutDelegate");

        reset(assignmentTimedOutDelegate);
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldTriggerTimeoutTaskAfterTimeout() throws Exception {

        final ProcessInstance processInstance = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));

        waitForAllJobs();

        verify(assignmentTimedOutDelegate).execute(argThat(withBusinessKey(caseId.toString())));
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldCancelAssignmentTimer() {

        final ProcessInstance processInstance = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));
        caseAssignmentTimeoutProcess.cancelTimer(caseId);

        verifyZeroInteractions(assignmentTimedOutDelegate);
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldFailGracefullyCancellingNonExistentAssignmentTimer() {

        caseAssignmentTimeoutProcess.cancelTimer(caseId);
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldNotThrowAnExceptionForDuplicateTimers() throws Exception {

        final ProcessInstance processInstance1 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));
        final ProcessInstance processInstance2 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));

        waitForAllJobs();

        verify(assignmentTimedOutDelegate, times(2)).execute(any());
        assertThat(isProcessFinished(processInstance1), equalTo(true));
        assertThat(isProcessFinished(processInstance2), equalTo(true));
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldCancelDuplicateTimers() {

        final ProcessInstance processInstance1 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));
        final ProcessInstance processInstance2 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(1));

        caseAssignmentTimeoutProcess.cancelTimer(caseId);

        assertThat(isProcessFinished(processInstance1), equalTo(true));
        assertThat(isProcessFinished(processInstance2), equalTo(true));
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldResetTimer() {

        ProcessInstance processInstance1 = caseAssignmentTimeoutProcess.startTimer(caseId, Duration.ofSeconds(MAX_MILLIS_TO_WAIT * 2));

        ProcessInstance processInstance2 = caseAssignmentTimeoutProcess.resetTimer(caseId, Duration.ofSeconds(1));

        waitForAllJobs();

        assertThat(isProcessFinished(processInstance1), equalTo(true));
        assertThat(isProcessFinished(processInstance2), equalTo(true));
    }

    private void waitForAllJobs() {
        waitForJobExecutorToProcessAllJobs(rule, MAX_MILLIS_TO_WAIT, 500);
    }

    private boolean isProcessFinished(final ProcessInstance processInstance) {
        final ProcessInstance refreshedProcessInstance = rule.getProcessEngine()
                .getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult();

        return refreshedProcessInstance == null;
    }
}
