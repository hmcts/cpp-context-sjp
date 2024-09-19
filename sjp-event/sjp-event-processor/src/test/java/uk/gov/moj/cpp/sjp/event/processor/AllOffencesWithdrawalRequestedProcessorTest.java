package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AllOffencesWithdrawalRequestedProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private Sender sender;

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private AllOffencesWithdrawalRequestedProcessor allOffencesWithdrawalRequestedProcessor;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Test
    public void shouldUpdateCaseState() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope privateEvent = createEnvelope(AllOffencesWithdrawalRequested.EVENT_NAME,
                createObjectBuilder().add(CASE_ID, caseId.toString()).build());

        allOffencesWithdrawalRequestedProcessor.handleAllOffencesWithdrawalEvent(privateEvent);
        verify(caseStateService).withdrawalRequested(caseId, privateEvent.metadata());
    }

    @Test
    public void shouldHandleAllOffencesWithdrawalRequested() {
        assertThat(AllOffencesWithdrawalRequestedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleAllOffencesWithdrawalEvent").thatHandles(AllOffencesWithdrawalRequested.EVENT_NAME)));
    }
}
