package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.AllOffencesWithdrawalRequestCancelledProcessor.WITHDRAWAL_REQUEST_CANCELLED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
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
public class AllOffencesWithdrawalRequestCancelledProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private Sender sender;

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private AllOffencesWithdrawalRequestCancelledProcessor allOffencesWithdrawalRequestCancelledProcessor;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Test
    public void shouldUpdateCaseState() {
        final UUID caseId = UUID.randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(AllOffencesWithdrawalRequestCancelled.EVENT_NAME,
                createObjectBuilder().add(CASE_ID, caseId.toString()).build());

        allOffencesWithdrawalRequestCancelledProcessor.handleWithdrawalRequestCancellation(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(WITHDRAWAL_REQUEST_CANCELLED_PUBLIC_EVENT_NAME));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(withJsonPath("$.caseId", equalTo(caseId.toString())))));

        verify(caseStateService).withdrawalRequestCancelled(caseId, privateEvent.metadata());
    }

    @Test
    public void shouldHandleAllOffencesWithdrawalRequested() {
        assertThat(AllOffencesWithdrawalRequestCancelledProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleWithdrawalRequestCancellation").thatHandles(AllOffencesWithdrawalRequestCancelled.EVENT_NAME)));
    }
}