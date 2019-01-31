package uk.gov.moj.cpp.sjp.event.processor.activiti;

import java.util.List;
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

    private RuntimeService runtimeService;

    @Inject
    ActivitiService(final RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    String startProcess(final String processName, final String businessKey, final Map<String, Object> params) {
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processName, businessKey, params);

        LOGGER.debug("Process {} with business key {} started with id {}", processName, businessKey, processInstance.getProcessInstanceId());

        return processInstance.getProcessInstanceId();
    }

    void signalProcess(final String processInstanceId, final String signalName, final Map<String, Object> params) {
        final List<Execution> signalEventSubscriptions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .signalEventSubscriptionName(signalName)
                .list();

        if (signalEventSubscriptions.isEmpty()) {
            LOGGER.warn("Process {} tried to send signal {} with {} but there are not any listeners for it", processInstanceId, signalName, params);
        } else {
            signalEventSubscriptions.forEach(execution ->
                    runtimeService.signalEventReceived(signalName, execution.getId(), params));

            LOGGER.debug("Signal {} sent to {} subscribers on process {}", signalName, signalEventSubscriptions.size(), processInstanceId);
        }
    }

    Optional<String> getProcessInstanceId(final String processDefinitionKey, final String businessKey) {
        return Optional.ofNullable(runtimeService.createExecutionQuery()
                .processInstanceBusinessKey(businessKey)
                .processDefinitionKey(processDefinitionKey)
                .singleResult()
        ).map(Execution::getProcessInstanceId);
    }

}