package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSJPSessionJsonObject;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSjpSessionId;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionProcessorTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    private final UUID caseId = randomUUID();
    private final UUID sjpSessionId = randomUUID();
    @Mock
    private Sender sender;
    @Mock
    private CaseStateService caseStateService;
    @InjectMocks
    private DecisionProcessor caseDecisionListener;
    @Mock
    private SjpService sjpService;
    @Mock
    private ResultingToResultsConverter converter;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldUpdateCaseState() {

        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved",
                Json.createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("resultedOn", now().toString())
                        .add("sjpSessionId", sjpSessionId.toString())
                        .build());
        caseDecisionListener.referencedDecisionsSaved(event);

        verify(sjpService).getSessionDetails(any(), any());
        verify(sjpService).getCaseDetails(any(), any());
        verify(sender).send(any());

        verify(caseStateService).caseCompleted(caseId, event.metadata());
    }

    @Test
    public void shouldUpdateCaseStateWithEnrichedJsonPayload() {

        final JsonEnvelope event = createReferenceDecisionSavedEvent();
        caseDecisionListener.referencedDecisionsSaved(event);

        verify(sjpService).getSessionDetails(any(), any());
        verify(sjpService).getCaseDetails(any(), any());
        verify(sender).send(any());
        verifyCaseStateService(event);
    }

    @Test
    public void shouldRaiseSJPCaseResultedEvent() {

        final JsonEnvelope event = createReferenceDecisionSavedEvent();

        when(sjpService.getSessionDetails(any(), any())).thenReturn(getSJPSessionJsonObject());
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(converter.convert(any(), any(), any(), any())).thenReturn(buildConverterResponse());

        caseDecisionListener.referencedDecisionsSaved(event);

        verify(sjpService).getSessionDetails(any(), any());
        verify(sjpService).getCaseDetails(any(), any());
        verify(converter).convert(any(), any(), any(), any());
        verify(sender).send(envelopeCaptor.capture());
        verifyCaseStateService(event);

        final JsonEnvelope envelope = envelopeCaptor.getValue();
        final JsonObject generatedEventPayload = envelope.payloadAsJsonObject();
        final JsonObject session = generatedEventPayload.getJsonObject("session");
        assertThat("public.sjp.case-resulted", equalTo(envelope.metadata().name()));
        assertThat(session.getString("sessionId"), equalTo(getSjpSessionId().toString()));
    }

    private JsonObject buildConverterResponse() {
        return createObjectBuilder()
                .add("session", createObjectBuilder()
                        .add("sessionId", getSjpSessionId().toString()))
                .build();

    }

    private void verifyCaseStateService(final JsonEnvelope event) {
        verify(caseStateService).caseCompleted(caseId, event.metadata());
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.accountDivisionCode"));
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.enforcingCourtCode"));
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.verdict"));
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.offences"));
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.offences[*].results"));
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.offences[*].results[*].code"));
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.offences[*].results[*].resultTypeId"));
        assertThat(event.payloadAsJsonObject(), hasJsonPath("$.offences[*].results[*].terminalEntries"));
    }

    private JsonEnvelope createReferenceDecisionSavedEvent() {

        final Integer accountDivisionCode = 199;
        final Integer enforcingCourtCode = 100;

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("resultedOn", now().toString())
                .add("sjpSessionId", sjpSessionId.toString())
                .add("verdict", "PSJ")
                .add("accountDivisionCode", accountDivisionCode)
                .add("enforcingCourtCode", enforcingCourtCode)
                .add("offences", Json.createArrayBuilder().add(
                        createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("results",
                                        createArrayBuilder().add(createObjectBuilder()
                                                .add("code", "")
                                                .add("resultTypeId", randomUUID().toString())
                                                .add("terminalEntries", "")))
                )).build();

        return createEnvelope("public.resulting.referenced-decisions-saved",
                payload);
    }

}