package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;


import java.time.ZonedDateTime;
import java.util.UUID;
import javax.json.JsonValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReserved;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyUnReserved;
import uk.gov.moj.cpp.sjp.event.CaseReserved;
import uk.gov.moj.cpp.sjp.event.CaseUnReserved;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

@ExtendWith(MockitoExtension.class)
public class CaseReservedProcessorTest {
    private final UUID caseId = UUID.randomUUID();
    private final UUID reservedBy = UUID.randomUUID();
    private final String caseURN = "CASEURN";
    private final ZonedDateTime reservedAt = ZonedDateTime.now();

    @Mock
    private Sender sender;

    @Mock
    private TimerService timerService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private CaseReservedProcessor caseReservedProcessor;

    @Test
    public void shouldRaiseCaseReservedPublicEvent(){
        final JsonEnvelope privateEvent = createEnvelope(CaseReserved.EVENT_NAME,
                createObjectBuilder().add("caseId", caseId.toString())
                        .add("caseUrn", caseURN)
                        .add("reservedAt", reservedAt.toString())
                        .add("reservedBy", reservedBy.toString())
                        .build());
        caseReservedProcessor.handleCaseReserved(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is("public.sjp.case-reserved"));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.caseUrn", equalTo(caseURN)),
                        withJsonPath("$.reservedAt", equalTo(reservedAt.toString())),
                        withJsonPath("$.reservedBy", equalTo(reservedBy.toString()))
                )));

        verify(timerService).startTimerForUndoReserveCase(eq(caseId), any(), eq(privateEvent.metadata()));
    }

    @Test
    public void shouldRaiseCaseAlreadyReservedPublicEvent(){
        final JsonEnvelope privateEvent = createEnvelope(CaseAlreadyReserved.EVENT_NAME,
                createObjectBuilder().add("caseId", caseId.toString())
                        .build());
        caseReservedProcessor.handleCaseAlreadyReserved(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is("public.sjp.case-already-reserved"));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString()))
                )));
    }

    @Test
    public void shouldRaiseCaseUnReservedPublicEvent(){
        final JsonEnvelope privateEvent = createEnvelope(CaseUnReserved.EVENT_NAME,
                createObjectBuilder().add("caseId", caseId.toString())
                        .add("caseUrn", caseURN)
                        .add("reservedBy", reservedBy.toString())
                        .build());
        caseReservedProcessor.handleCaseUnReserved(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is("public.sjp.case-unreserved"));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.caseUrn", equalTo(caseURN)),
                        withJsonPath("$.reservedBy", equalTo(reservedBy.toString()))
                )));
    }

    @Test
    public void shouldRaiseCaseAlreadyUnReservedPublicEvent(){
        final JsonEnvelope privateEvent = createEnvelope(CaseAlreadyUnReserved.EVENT_NAME,
                createObjectBuilder().add("caseId", caseId.toString())
                        .build());
        caseReservedProcessor.handleCaseAlreadyUnReserved(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is("public.sjp.case-already-unreserved"));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString()))
                )));
    }

    @Test
    public void shouldRaiseCaseReserveFailedAsAlreadyCompletedPublicEvent() {
        final JsonEnvelope privateEvent = createEnvelope("sjp.case-reserve-failed-as-already-completed",
                createObjectBuilder().add("caseId", caseId.toString()).build());

        caseReservedProcessor.handleCaseReservedFailedAsAlreadyCompleted(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is("public.sjp.case-reserve-failed-as-already-completed"));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString()))));
    }
}
