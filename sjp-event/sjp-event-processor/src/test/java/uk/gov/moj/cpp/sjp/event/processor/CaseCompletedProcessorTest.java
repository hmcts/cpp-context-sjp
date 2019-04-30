package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseCompletedProcessorTest {

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private CaseCompletedProcessor caseCompletedProcessor;

    @Test
    public void shouldSignalCaseStatusProcessThatCaseHasBeenCompleted() {
        final UUID caseId = randomUUID();
        final JsonEnvelope envelope = createEnvelope("sjp.events.case-completed",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        caseCompletedProcessor.handleCaseCompleted(envelope);

        verify(caseStateService).caseCompleted(caseId, envelope.metadata());
    }

    @Test
    public void shouldHandleCaseCompletedEvent() {
        assertThat(CaseCompletedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleCaseCompleted").thatHandles("sjp.events.case-completed")));
    }
}
