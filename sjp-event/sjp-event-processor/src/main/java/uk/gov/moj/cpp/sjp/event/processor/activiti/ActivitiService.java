package uk.gov.moj.cpp.sjp.event.processor.activiti;

import java.util.Map;
import java.util.Optional;

import javax.ejb.Startup;
import javax.inject.Inject;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
public class ActivitiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiService.class);

    @Inject
    private RuntimeService runtimeService;

    public String startProcess(final String processName, final String businessKey, final Map<String, Object> params) {
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processName, businessKey, params);

        LOGGER.debug("Process {} with business key {} started with id {}", processName, businessKey, processInstance.getProcessInstanceId());

        return processInstance.getProcessInstanceId();
    }

    public void signalProcess(final String processInstanceId, final String signalName, final Map<String, Object> params) {
        runtimeService.createExecutionQuery().processInstanceId(processInstanceId)
                .signalEventSubscriptionName(signalName)
                .list().forEach(execution -> runtimeService.signalEventReceived(signalName, execution.getId(), params));

        LOGGER.debug("Signal {} sent to process with id {}", signalName, processInstanceId);
    }

    public Optional<String> getProcessInstanceId(final String processDefinitionKey, final String businessKey) {
        return Optional.ofNullable(runtimeService.createExecutionQuery()
                .processInstanceBusinessKey(businessKey)
                .processDefinitionKey(processDefinitionKey)
                .singleResult()
        ).map(Execution::getProcessInstanceId);
    }

}