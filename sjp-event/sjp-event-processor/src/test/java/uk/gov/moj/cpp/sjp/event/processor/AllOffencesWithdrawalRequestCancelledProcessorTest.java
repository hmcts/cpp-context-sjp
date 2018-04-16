package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AllOffencesWithdrawalRequestCancelledProcessorTest {

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private AllOffencesWithdrawalRequestCancelledProcessor allOffencesWithdrawalRequestCancelledProcessor;

    @Test
    public void shouldUpdateCaseState() {

        final UUID caseId = UUID.randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(AllOffencesWithdrawalRequestCancelled.EVENT_NAME,
                createObjectBuilder().add(CASE_ID, caseId.toString()).build());

        allOffencesWithdrawalRequestCancelledProcessor.handleWithdrawalRequestCancellation(privateEvent);

        verify(caseStateService).withdrawalRequestCancelled(caseId, privateEvent.metadata());
    }

    @Test
    public void shouldHandleAllOffencesWithdrawalRequested() {
        assertThat(AllOffencesWithdrawalRequestCancelledProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleWithdrawalRequestCancellation").thatHandles(AllOffencesWithdrawalRequestCancelled.EVENT_NAME)));
    }
}