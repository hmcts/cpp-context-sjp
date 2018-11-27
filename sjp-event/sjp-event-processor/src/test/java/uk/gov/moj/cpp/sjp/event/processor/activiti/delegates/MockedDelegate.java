package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockedDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockedDelegate.class);

    private String delegateName;
    private List<DelegateExecution> delegateExecutions;
    private Map<String, Object> variablesOnExecution;
    private List<String> variablesToRemoveOnExecution;

    public MockedDelegate(final Class<? extends JavaDelegate> delegateName) {
        this.delegateName = delegateName.getSimpleName();
        resetExecutions();
    }

    @Override
    public void execute(final DelegateExecution execution) {
        // add/remove extra variables on runtime
        ofNullable(variablesOnExecution).ifPresent(execution::setVariables);
        ofNullable(variablesToRemoveOnExecution).ifPresent(execution::removeVariables);

        // create an history of all the executions
        delegateExecutions.add(execution);

        LOGGER.info("DelegateExecution[" + delegateName + "][" + getExecutionTimes() + "][" + execution.getProcessBusinessKey() + "] called with " + execution.getVariables());
    }

    public int getExecutionTimes() {
        return delegateExecutions.size();
    }

    public List<DelegateExecution> getDelegateExecutions() {
        return delegateExecutions;
    }

    public void resetExecutions() {
        this.delegateExecutions = new ArrayList<>();
    }

    /**
     * Specify extra parameters to inject during execution
     * WARNING: this is a singleton bean, once set it will affect all the executions, if required improve it
     */
    public void setVariablesOnExecution(final Map<String, Object> variablesOnExecution) {
        this.variablesOnExecution = variablesOnExecution;
    }

    public Map<String, Object> getVariablesOnExecution() {
        return variablesOnExecution;
    }

    /**
     * Specify extra parameters to inject during execution
     */
    public void setVariablesToRemoveOnExecution(final List<String> variablesToRemoveOnExecution) {
        this.variablesToRemoveOnExecution = variablesToRemoveOnExecution;
    }

}