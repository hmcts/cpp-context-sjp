package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.POSTING_DATE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReceivedProcessorTest {

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private CaseReceivedProcessor caseReceivedProcessor;

    @Test
    public void shouldUpdateCaseState() {

        final UUID caseId = UUID.randomUUID();
        final LocalDate postingDate = LocalDate.now();

        final JsonEnvelope privateEvent = createEnvelope(CaseReceived.EVENT_NAME,
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(POSTING_DATE, postingDate.toString())
                        .build());

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(caseStateService).caseReceived(caseId, postingDate, privateEvent.metadata());
    }

    @Test
    public void shouldHandleCaseReceivedEvent() {
        assertThat(CaseReceivedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleCaseReceivedEvent").thatHandles(CaseReceived.EVENT_NAME)));
    }
}