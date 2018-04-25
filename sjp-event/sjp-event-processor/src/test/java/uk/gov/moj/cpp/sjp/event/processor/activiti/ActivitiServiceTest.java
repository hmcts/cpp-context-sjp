package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ActivitiServiceTest {

    private static final String BUSINESS_KEY = "businessKey";
    private static final String PROCESS_DEFINITION_KEY = "processDefinitionKey";

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ExecutionQuery executionQuery;

    @Mock
    private Execution execution;

    @InjectMocks
    private ActivitiService activitiService;

    @Test
    public void shouldStartProcess() {
        final Map<String, Object> params = emptyMap();
        final String processInstanceId = randomAlphabetic(10);

        when(processInstance.getProcessInstanceId()).thenReturn(processInstanceId);
        when(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, BUSINESS_KEY, params)).thenReturn(processInstance);

        final String createdProcessInstanceId = activitiService.startProcess(PROCESS_DEFINITION_KEY, BUSINESS_KEY, params);

        assertThat(createdProcessInstanceId, is(processInstanceId));
        verify(runtimeService).startProcessInstanceByKey(PROCESS_DEFINITION_KEY, BUSINESS_KEY, params);
    }

    @Test
    public void shouldReturnOptionalProcessInstanceIdIfProcessExists() {
        final String processInstanceId = randomAlphabetic(10);

        when(executionQuery.processInstanceBusinessKey(BUSINESS_KEY)).thenReturn(executionQuery);
        when(executionQuery.processDefinitionKey(PROCESS_DEFINITION_KEY)).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);

        final Optional<String> optionalProcessInstanceId = activitiService.getProcessInstanceId(PROCESS_DEFINITION_KEY, BUSINESS_KEY);

        assertThat(optionalProcessInstanceId.get(), is(processInstanceId));
    }

    @Test
    public void shouldReturnEmptyProcessInstanceIdIfProcessDoesNotExist() {
        when(executionQuery.processInstanceBusinessKey(BUSINESS_KEY)).thenReturn(executionQuery);
        when(executionQuery.processDefinitionKey(PROCESS_DEFINITION_KEY)).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(null);
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);

        final Optional<String> optionalProcessInstanceId = activitiService.getProcessInstanceId(PROCESS_DEFINITION_KEY, BUSINESS_KEY);

        assertThat(optionalProcessInstanceId.isPresent(), is(false));
    }
}
