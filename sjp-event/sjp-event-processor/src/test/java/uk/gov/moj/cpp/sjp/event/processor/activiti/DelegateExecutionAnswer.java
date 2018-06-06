package uk.gov.moj.cpp.sjp.event.processor.activiti;


import org.activiti.engine.delegate.DelegateExecution;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@FunctionalInterface
public interface DelegateExecutionAnswer<T> extends Answer<Void> {

    @Override
    default Void answer(InvocationOnMock invocationOnMock) {
        answer(invocationOnMock.getArgumentAt(0, DelegateExecution.class));
        return null;
    }

    void answer(final DelegateExecution delegateExecution);
}
