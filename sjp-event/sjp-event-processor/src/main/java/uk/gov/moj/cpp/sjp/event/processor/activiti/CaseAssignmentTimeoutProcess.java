package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;

@SuppressWarnings("WeakerAccess")
@Named
public class CaseAssignmentTimeoutProcess {

    private static final String CASE_TIMEOUT_PROCESS_NAME = "sjpCaseAssignmentTimeout";
    private static final Logger LOGGER = getLogger(CaseAssignmentTimeoutProcess.class);
    private static final String DURATION = "duration";
    private final RuntimeService runtimeService;

    @Inject
    public CaseAssignmentTimeoutProcess(final RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public ProcessInstance startTimer(final UUID caseId, final Duration duration) {

        final Map<String, Object> params = Collections.singletonMap(DURATION, duration.toString());

        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CASE_TIMEOUT_PROCESS_NAME, caseId.toString(), params);

        LOGGER.info("Case assignment timeout started for case {} ", caseId);

        return processInstance;
    }

    public void cancelTimer(final UUID caseId) {

        runtimeService
                .createExecutionQuery()
                .processDefinitionKey(CASE_TIMEOUT_PROCESS_NAME)
                .processInstanceBusinessKey(caseId.toString())
                .list()
                .forEach(processInstance -> {
                            runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), "Timeout cancelled");
                            LOGGER.info("Case assignment timeout cancelled for case {} ", caseId);
                        }
                );
    }

    public ProcessInstance resetTimer(UUID caseId, Duration caseTimeoutDuration) {
        cancelTimer(caseId);
        return startTimer(caseId, caseTimeoutDuration);
    }
}
