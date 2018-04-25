package uk.gov.moj.cpp.sjp.event.processor.activiti;


import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;


public class DelegateExecutionStubber {

    public static Stubber mockDelegate(final DelegateExecutionAnswer delegateExecutionAnswer) {
        return Mockito.doAnswer(delegateExecutionAnswer);
    }
}
