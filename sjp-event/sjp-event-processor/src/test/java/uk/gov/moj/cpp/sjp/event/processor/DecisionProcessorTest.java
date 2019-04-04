package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionProcessorTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private DecisionProcessor caseDecisionListener;

    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;

    private UUID caseId = randomUUID();


    @Test
    public void shouldUpdateCaseState() {

        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved",
                Json.createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("resultedOn", now().toString())
                        .add("sjpSessionId", randomUUID().toString())
                        .build());
        caseDecisionListener.referencedDecisionsSaved(event);

        verify(caseStateService).caseCompleted(caseId, event.metadata());
    }

    @Test
    public void shouldUpdateCaseStateWithEnrichedJsonPayload() {

        final Integer accountDivisionCode = 199;
        final Integer enforcingCourtCode = 100;

        JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("resultedOn", now().toString())
                .add("sjpSessionId", randomUUID().toString())
                .add("verdict", "PSJ")
                .add("accountDivisionCode",accountDivisionCode)
                .add("enforcingCourtCode",enforcingCourtCode)
                .add("offences", Json.createArrayBuilder().add(
                        createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("results",
                                        createArrayBuilder().add(createObjectBuilder()
                                                .add("code", "")
                                                .add("resultTypeId", randomUUID().toString())
                                                .add("terminalEntries", "")))
                )).build();
        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved",

                        payload);
        caseDecisionListener.referencedDecisionsSaved(event);

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

    @Test
    public void shouldUpdateCaseStateWithEnrichedJsonPayloadWithPublicEvent() {

        final JsonObject payload =
                Json.createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("hearing", "")
                        .add("variants", "")
                        .add("sharedTime", now().toString())
                        .build();

        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved", payload);
        caseDecisionListener.referencedDecisionsSaved(event);

        verify(caseStateService).caseCompleted(caseId, event.metadata());
        verify(sender,times(1)).send(Mockito.any(Envelope.class));
        verify(sender).send(captor.capture());

        JsonEnvelope publicEvent = captor.getValue();
        assertNotNull(publicEvent);
        assertThat(publicEvent, jsonEnvelope(
                metadata().withName("public.sjp.case-results"),
                payloadIsJson(allOf(withJsonPath("$.caseId", equalTo(caseId.toString()))))));

    }


}