package uk.gov.moj.cpp.sjp.event.processor.matcher;

import org.activiti.engine.delegate.DelegateExecution;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;


public class DelegateExecutionProcessBusinessKeyMatcher extends TypeSafeDiagnosingMatcher<DelegateExecution> {

    private final String businessKey;

    private DelegateExecutionProcessBusinessKeyMatcher(final String businessKey) {
        this.businessKey = businessKey;
    }

    public static DelegateExecutionProcessBusinessKeyMatcher withProcessBusinessKey(final String businessKey) {
        return new DelegateExecutionProcessBusinessKeyMatcher(businessKey);
    }

    @Override
    protected boolean matchesSafely(final DelegateExecution delegateExecution, final Description mismatchDescription) {
        return delegateExecution.getProcessBusinessKey().equals(businessKey);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("Delegation execution expected to have business key").appendValue(businessKey);
    }
}
