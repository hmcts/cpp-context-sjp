package uk.gov.moj.cpp.sjp.event.processor.matcher;

import org.activiti.engine.delegate.DelegateExecution;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;


public class DelegateExecutionBusinessKeyMatcher extends TypeSafeDiagnosingMatcher<DelegateExecution> {

    private final String businessKey;

    public static DelegateExecutionBusinessKeyMatcher withBusinessKey(final String businessKey) {
        return new DelegateExecutionBusinessKeyMatcher(businessKey);
    }

    private DelegateExecutionBusinessKeyMatcher(final String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    protected boolean matchesSafely(final DelegateExecution delegateExecution, final Description mismatchDescription) {
        return delegateExecution.getProcessBusinessKey().equals(businessKey);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("Delegation execution expected to have business key ").appendValue(businessKey);
    }
}
