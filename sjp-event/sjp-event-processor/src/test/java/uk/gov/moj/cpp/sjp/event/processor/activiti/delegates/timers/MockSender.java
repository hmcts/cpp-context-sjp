package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.timers;

import uk.gov.justice.services.core.sender.Sender;

import org.mockito.Mockito;

public class MockSender {
    public static Sender sender = Mockito.mock(Sender.class);
}
