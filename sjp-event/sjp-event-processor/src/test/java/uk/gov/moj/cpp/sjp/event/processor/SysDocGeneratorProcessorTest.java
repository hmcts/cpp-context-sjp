package uk.gov.moj.cpp.sjp.event.processor;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.SystemDocGeneratorResponseStrategy;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.SystemDocGeneratorEnvelopes;

import java.util.Arrays;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SysDocGeneratorProcessorTest {

    @Mock
    private SystemDocGeneratorResponseStrategy strategy1;
    @Mock
    private SystemDocGeneratorResponseStrategy strategy2;
    @Mock
    private Instance<SystemDocGeneratorResponseStrategy> strategies;
    @InjectMocks
    private SysDocGeneratorProcessor processor;

    @Test
    public void shouldCallStrategyThatCanProcessThePayloadForDocumentAvailableEvent() {
        final JsonEnvelope envelope = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent().envelope();
        when(strategy1.canProcess(envelope)).thenReturn(true);
        when(strategies.iterator()).thenReturn(Arrays.asList(strategy1, strategy2).iterator());

        processor.handleDocumentAvailableEvent(envelope);

        verify(strategy1).process(envelope);
        verify(strategy2, never()).process(envelope);
    }

    @Test
    public void shouldCallStrategyThatCanProcessThePayloadForGenerationFailedEvent() {
        final JsonEnvelope envelope = SystemDocGeneratorEnvelopes.generationFailedEvent().envelope();
        when(strategy2.canProcess(envelope)).thenReturn(true);
        when(strategies.iterator()).thenReturn(Arrays.asList(strategy2, strategy1).iterator());

        processor.handleDocumentGenerationFailedEvent(envelope);

        verify(strategy2).process(envelope);
        verify(strategy1, never()).process(envelope);
    }

}