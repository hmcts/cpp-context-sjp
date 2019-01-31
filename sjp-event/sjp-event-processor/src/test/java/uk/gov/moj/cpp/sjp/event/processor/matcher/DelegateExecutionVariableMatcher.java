package uk.gov.moj.cpp.sjp.event.processor.matcher;

import java.util.Objects;

import org.activiti.engine.delegate.DelegateExecution;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;


public class DelegateExecutionVariableMatcher extends TypeSafeDiagnosingMatcher<DelegateExecution> {

    private final String variableName;
    private final Object variableValue;

    public static DelegateExecutionVariableMatcher withProcessVariable(final String variableName, final Object variableValue) {
        return new DelegateExecutionVariableMatcher(variableName, variableValue);
    }

    public static DelegateExecutionVariableMatcher withoutProcessVariable(final String variableName) {
        return new DelegateExecutionVariableMatcher(variableName, null);
    }

    private DelegateExecutionVariableMatcher(final String variableName, final Object variableValue) {
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    @Override
    protected boolean matchesSafely(final DelegateExecution delegateExecution, final Description mismatchDescription) {
        boolean variableNotPresent = !delegateExecution.hasVariable(variableName);
        if (variableValue == null && variableNotPresent) {
            return true;
        }

        if (variableNotPresent) {
            mismatchDescription.appendText("but variable ").appendValue(variableName).appendText(" was missing");
            return false;
        } else {
            final Object actualVariableValue = delegateExecution.getVariable(variableName);
            if (Objects.equals(variableValue, actualVariableValue)) {
                return true;
            } else {
                mismatchDescription.appendText("but was ").appendValue(actualVariableValue);
                return false;
            }
        }
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("Delegation execution expect to contains ").appendValue(variableValue).appendText(" under variableName name ").appendValue(variableName);
    }
}
