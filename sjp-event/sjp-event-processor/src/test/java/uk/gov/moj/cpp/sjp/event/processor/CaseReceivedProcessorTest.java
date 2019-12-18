package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.CaseReceivedProcessor.CASE_STARTED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.EXPECTED_DATE_READY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.POSTING_DATE;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReceivedProcessorTest {

    @InjectMocks
    private CaseReceivedProcessor caseReceivedProcessor;

    @Mock
    protected Sender sender;

    @Mock
    private TimerService timerService;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Test
    public void shouldUpdateCaseState() {
        final UUID caseId = randomUUID();
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createEnvelope(CaseReceived.EVENT_NAME,
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(POSTING_DATE, postingDate.toString())
                        .add(EXPECTED_DATE_READY, expectedDateReady.toString())
                        .build());

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), equalTo(CASE_STARTED_PUBLIC_EVENT_NAME));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.id", equalTo(caseId.toString())),
                        withJsonPath("$.postingDate", equalTo(postingDate.toString())))));

        verify(timerService).startTimerForDefendantResponse(caseId, expectedDateReady, privateEvent.metadata());
    }

    @Test
    public void shouldHandleCaseReceivedEvent() {
        assertThat(CaseReceivedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleCaseReceivedEvent").thatHandles(CaseReceived.EVENT_NAME)));
    }
}
