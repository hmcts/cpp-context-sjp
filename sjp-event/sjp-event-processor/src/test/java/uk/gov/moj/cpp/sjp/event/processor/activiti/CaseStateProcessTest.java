package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.UUID.randomUUID;
import static org.activiti.engine.impl.test.JobTestHelper.waitForJobExecutorToProcessAllJobs;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_COMPLETED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.IS_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.NOTICE_ENDED_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_CANCELLED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_UPDATED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegateExecutionStubber.mockDelegate;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.DelegateExecutionBusinessKeyMatcher.withBusinessKey;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.DelegateExecutionVariableMatcher.withProcessVariable;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class CaseStateProcessTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseStateProcessTest.class);

    private static final String PROCESS_PATH = "processes/case-state.bpmn20.xml";
    private RuntimeService runtimeService;

    @Rule
    public ActivitiRule rule = new ActivitiRule();

    private JavaDelegate caseStartedDelegate, provedInAbsenceDelegate, readyCaseDelegate,
            pleaUpdatedDelegate, pleaCancelledDelegate, withdrawalRequestedDelegate,
            withdrawalRequestCancelledDelegate, caseCompletedDelegate;

    private MetadataHelper metadataHelper = new MetadataHelper();

    private UUID caseId;
    private Metadata metadata;

    @Before
    public void init() {
        caseId = randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        final StandaloneProcessEngineConfiguration configuration = (StandaloneProcessEngineConfiguration)
                rule.getProcessEngine().getProcessEngineConfiguration();

        final Map<Object, Object> beans = configuration.getBeans();

        caseStartedDelegate = (JavaDelegate) beans.get("caseStartedDelegate");
        provedInAbsenceDelegate = (JavaDelegate) beans.get("provedInAbsenceDelegate");
        readyCaseDelegate = (JavaDelegate) beans.get("readyCaseDelegate");
        pleaUpdatedDelegate = (JavaDelegate) beans.get("pleaUpdatedDelegate");
        pleaCancelledDelegate = (JavaDelegate) beans.get("pleaCancelledDelegate");
        withdrawalRequestedDelegate = (JavaDelegate) beans.get("withdrawalRequestedDelegate");
        withdrawalRequestCancelledDelegate = (JavaDelegate) beans.get("withdrawalRequestCancelledDelegate");
        caseCompletedDelegate = (JavaDelegate) beans.get("caseCompletedDelegate");
        runtimeService = rule.getRuntimeService();

        reset(caseStartedDelegate,
                provedInAbsenceDelegate,
                readyCaseDelegate,
                pleaUpdatedDelegate,
                pleaCancelledDelegate,
                withdrawalRequestedDelegate,
                withdrawalRequestCancelledDelegate,
                caseCompletedDelegate);
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldExecuteCaseStartedDelegateWhenProcessStarted() throws Exception {

        final LocalDate postingDate = LocalDate.now().plusDays(1);

        final ProcessInstance processInstance = startProcess(caseId, postingDate, metadata);

        tryProcessPendingJobs();

        verify(caseStartedDelegate).execute(argThat(allOf(
                withBusinessKey(caseId.toString()),
                withProcessVariable(POSTING_DATE_VARIABLE, postingDate.toString()),
                withProcessVariable(METADATA_VARIABLE, metadataHelper.metadataToString(metadata))
        )));

        verifyZeroInteractions(
                provedInAbsenceDelegate,
                readyCaseDelegate,
                pleaUpdatedDelegate,
                pleaCancelledDelegate,
                withdrawalRequestedDelegate,
                withdrawalRequestCancelledDelegate,
                caseCompletedDelegate);

        assertThat(isProcessFinished(processInstance), equalTo(false));
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldExecuteWithdrawalRelatedDelegates() throws Exception {

        final LocalDate postingDate = LocalDate.now().plusDays(1);

        mockDelegate(de -> de.setVariable(IS_READY_VARIABLE, true))
                .when(readyCaseDelegate).execute(Matchers.any(DelegateExecution.class));

        final ProcessInstance processInstance = startProcess(caseId, postingDate, metadata);

        requestWithdrawal(processInstance, metadata);

        tryProcessPendingJobs();

        verify(withdrawalRequestedDelegate).execute(argThat(withProcessVariable(METADATA_VARIABLE, metadataHelper.metadataToString(metadata))));
        verify(readyCaseDelegate).execute(argThat(any(DelegateExecution.class)));

        cancelWithdrawalRequest(processInstance, metadata);

        tryProcessPendingJobs();

        verify(withdrawalRequestCancelledDelegate).execute(argThat(withProcessVariable(METADATA_VARIABLE, metadataHelper.metadataToString(metadata))));

        verify(readyCaseDelegate, times(2)).execute(argThat(any(DelegateExecution.class)));

        verifyZeroInteractions(
                provedInAbsenceDelegate,
                pleaUpdatedDelegate,
                pleaCancelledDelegate,
                caseCompletedDelegate);

        assertThat(isProcessFinished(processInstance), equalTo(false));
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldExecutePleaRelatedDelegates() throws Exception {

        final UUID offenceId = randomUUID();
        final LocalDate postingDate = LocalDate.now().plusDays(1);
        final PleaType pleaType = PleaType.GUILTY;

        mockDelegate(de -> de.setVariable(IS_READY_VARIABLE, true))
                .when(readyCaseDelegate).execute(Matchers.any(DelegateExecution.class));

        final ProcessInstance processInstance = startProcess(caseId, postingDate, metadata);

        pleaUpdated(processInstance, offenceId, PleaType.GUILTY, metadata);

        tryProcessPendingJobs();

        verify(pleaUpdatedDelegate).execute(argThat(allOf(
                withProcessVariable(METADATA_VARIABLE, metadataHelper.metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name())
        )));

        verify(readyCaseDelegate).execute(argThat(any(DelegateExecution.class)));

        pleaCancelled(processInstance, offenceId, metadata);

        tryProcessPendingJobs();

        verify(pleaCancelledDelegate).execute(argThat(allOf(
                withProcessVariable(METADATA_VARIABLE, metadataHelper.metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString())
        )));

        verify(readyCaseDelegate, times(2)).execute(argThat(any(DelegateExecution.class)));

        verifyZeroInteractions(
                provedInAbsenceDelegate,
                withdrawalRequestedDelegate,
                withdrawalRequestCancelledDelegate,
                caseCompletedDelegate);

        assertThat(isProcessFinished(processInstance), equalTo(false));
    }


    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldExecuteProvedInAbsenceDelegateWhenPostingDateIsOlderThan28Days() throws Exception {

        final LocalDate postingDate = LocalDate.now().minusDays(29);

        mockDelegate(de -> de.setVariable(IS_READY_VARIABLE, true))
                .when(readyCaseDelegate).execute(Matchers.any(DelegateExecution.class));

        final ProcessInstance processInstance = startProcess(caseId, postingDate, metadata);

        tryProcessPendingJobs();

        verify(provedInAbsenceDelegate).execute(argThat(any(DelegateExecution.class)));

        verifyZeroInteractions(
                pleaUpdatedDelegate,
                pleaCancelledDelegate,
                withdrawalRequestedDelegate,
                withdrawalRequestCancelledDelegate,
                caseCompletedDelegate);

        assertThat(isProcessFinished(processInstance), equalTo(false));
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldNotExecuteProvedInAbsenceDelegateWhenPostingDateIsYoungerThan28Days() throws Exception {

        final LocalDate postingDate = LocalDate.now().minusDays(27);

        final ProcessInstance processInstance = startProcess(caseId, postingDate, metadata);

        tryProcessPendingJobs();

        verifyZeroInteractions(
                provedInAbsenceDelegate,
                readyCaseDelegate,
                pleaUpdatedDelegate,
                pleaCancelledDelegate,
                withdrawalRequestedDelegate,
                withdrawalRequestCancelledDelegate,
                caseCompletedDelegate);

        assertThat(isProcessFinished(processInstance), equalTo(false));
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldNotCompleteProcessIfNotInReadyState() throws Exception {

        final LocalDate postingDate = LocalDate.now().plusDays(1);

        final ProcessInstance processInstance = startProcess(caseId, postingDate, metadata);

        caseCompleted(processInstance, metadata);

        tryProcessPendingJobs();

        verifyZeroInteractions(
                provedInAbsenceDelegate,
                readyCaseDelegate,
                pleaUpdatedDelegate,
                pleaCancelledDelegate,
                withdrawalRequestedDelegate,
                withdrawalRequestCancelledDelegate,
                caseCompletedDelegate);

        tryProcessPendingJobs();

        assertThat(isProcessFinished(processInstance), equalTo(false));
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldCompleteProcessIfInReadyState() throws Exception {

        final LocalDate postingDate = LocalDate.now().minusDays(30);

        mockDelegate(de -> de.setVariable(IS_READY_VARIABLE, true))
                .when(readyCaseDelegate).execute(Matchers.any(DelegateExecution.class));

        final ProcessInstance processInstance = startProcess(caseId, postingDate, metadata);

        tryProcessPendingJobs();

        caseCompleted(processInstance, metadata);

        tryProcessPendingJobs();

        verify(caseCompletedDelegate).execute(argThat(withProcessVariable(METADATA_VARIABLE, metadataHelper.metadataToString(metadata))));

        verifyZeroInteractions(
                pleaUpdatedDelegate,
                pleaCancelledDelegate,
                withdrawalRequestedDelegate,
                withdrawalRequestCancelledDelegate);

        tryProcessPendingJobs();

        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    private ProcessInstance startProcess(final UUID caseId, final LocalDate postingDate, final Metadata metadata) {
        final Map<String, Object> processParams = new HashMap<>();
        processParams.put(NOTICE_ENDED_DATE_VARIABLE, postingDate.plusDays(28).atStartOfDay().format(ISO_DATE_TIME));
        processParams.put(POSTING_DATE_VARIABLE, postingDate.format(ISO_DATE));
        processParams.put(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));

        return runtimeService.startProcessInstanceByKey(PROCESS_NAME, caseId.toString(), processParams);
    }

    private void pleaUpdated(final ProcessInstance processInstance, final UUID offenceId, final PleaType pleaType, final Metadata metadata) {
        final Map<String, Object> processParams = new HashMap<>();
        processParams.put(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));
        processParams.put(OFFENCE_ID_VARIABLE, offenceId.toString());
        processParams.put(PLEA_TYPE_VARIABLE, pleaType.name());

        signalProcess(processInstance, PLEA_UPDATED_SIGNAL_NAME, processParams);
    }

    private void pleaCancelled(final ProcessInstance processInstance, final UUID offenceId, final Metadata metadata) {
        final Map processParams = ImmutableMap.of(METADATA_VARIABLE, metadataHelper.metadataToString(metadata), OFFENCE_ID_VARIABLE, offenceId.toString());
        signalProcess(processInstance, PLEA_CANCELLED_SIGNAL_NAME, processParams);
    }

    private void requestWithdrawal(final ProcessInstance processInstance, final Metadata metadata) {
        final Map<String, Object> processParams = ImmutableMap.of(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));
        signalProcess(processInstance, WITHDRAWAL_REQUESTED_SIGNAL_NAME, processParams);
    }

    private void cancelWithdrawalRequest(final ProcessInstance processInstance, final Metadata metadata) {
        final Map<String, Object> processParams = ImmutableMap.of(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));
        signalProcess(processInstance, WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME, processParams);
    }

    private void caseCompleted(final ProcessInstance processInstance, final Metadata metadata) {
        final Map<String, Object> processParams = ImmutableMap.of(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));
        signalProcess(processInstance, CASE_COMPLETED_SIGNAL_NAME, processParams);
    }

    private void signalProcess(final ProcessInstance processInstance, final String signalName, final Map<String, Object> processParams) {
        runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .signalEventSubscriptionName(signalName)
                .list()
                .forEach(execution -> runtimeService.signalEventReceived(signalName, execution.getId(), processParams));
    }

    private boolean isProcessFinished(final ProcessInstance processInstance) {
        final ProcessInstance refreshedProcessInstance = rule.getProcessEngine()
                .getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult();

        return refreshedProcessInstance == null;
    }

    private void tryProcessPendingJobs() {
        try {
            waitForJobExecutorToProcessAllJobs(rule, 1000, 10);
        } catch (final ActivitiException e) {
            LOGGER.warn("Exception thrown while waiting for jobs to be processed", e);
        }
    }
}
