package uk.gov.moj.cpp.sjp.domain.transformation;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.*;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.transformation.converter.ResultingDecisionsConverter;
import uk.gov.moj.cpp.sjp.domain.transformation.service.ResultingService;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpEventStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionsTransformerTest {

    private DecisionsTransformer decisionsTransformer;

    @Mock
    private ResultingService resultingService;

    @Mock
    private SjpEventStoreService sjpEventStoreService;

    @Mock
    private SjpViewStoreService sjpViewStoreService;

    @Mock
    private ResultingDecisionsConverter resultingDecisionsConverter;

    @Before
    public void setup() {
        decisionsTransformer = new DecisionsTransformer(
                sjpEventStoreService,
                sjpViewStoreService,
                resultingDecisionsConverter,
                resultingService);
        decisionsTransformer.setEnveloper(EnveloperFactory.createEnveloper());
    }

    @Test
    public void shouldTransformCaseCompletedEvent() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final JsonEnvelope caseCompleted = jsonEnvelope(caseId, decisionId, CaseCompleted.EVENT_NAME);

        final Action action = decisionsTransformer.actionFor(caseCompleted);

        assertThat("Expected action type is TRANSFORM for case-completed event", action,
                Matchers.equalTo(Action.TRANSFORM));
    }

    @Test
    public void shouldNotTransformCaseReceivedEvent() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final JsonEnvelope caseReceived = jsonEnvelope(caseId, decisionId, CaseReceived.EVENT_NAME);

        final Action action = decisionsTransformer.actionFor(caseReceived);

        assertThat("Expected action type is NO_ACTION for case-received event", action,
                Matchers.equalTo(Action.NO_ACTION));
    }


    @Test
    public void shouldCreateNewDecisionTransformedEventForCaseCompletedEvents() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();

        final JsonEnvelope caseCompleted = jsonEnvelope(caseId, decisionId, CaseCompleted.EVENT_NAME);
        final JsonObject decisionPayload = mock(JsonObject.class);
        final JsonEnvelope decisionSavedEvent = mock(JsonEnvelope.class);

        when(resultingService.getDecisionForACase(caseId)).thenReturn(decisionPayload);
        when(resultingDecisionsConverter.convert(caseCompleted, decisionPayload)).thenReturn(decisionSavedEvent);
        when(sjpViewStoreService.getStatus(caseId.toString())).thenReturn(Optional.of(CaseStatus.REFERRED_FOR_COURT_HEARING.toString()));

        // when
        final Stream<JsonEnvelope> transformedEventsStream = decisionsTransformer.apply(caseCompleted);

        // then
        verify(resultingService, times(1)).getDecisionForACase(caseId);
        verify(resultingDecisionsConverter, times(1)).convert(caseCompleted, decisionPayload);

        List<JsonEnvelope> eventList = transformedEventsStream.collect(Collectors.toList());

        final JsonEnvelope caseStatusChanged = envelopeFrom(
                metadataOf(eventList.get(2).metadata().id().toString(), "sjp.events.case-status-changed")
                        .build(),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("caseStatus", CaseStatus.COMPLETED.toString())
                        .build());

        assertThat(eventList.get(0), is(decisionSavedEvent));
        assertThat(eventList.get(1), is(caseCompleted));

        assertThat(eventList.get(2).metadata(),
                metadata().of(caseStatusChanged.metadata())
                        .isJson(allOf(withJsonPath("id", notNullValue()),
                                withJsonPath("name", is("sjp.events.case-status-changed")))));

        assertThat(eventList.get(2).payloadAsJsonObject().toString(),
                isJson(allOf(
                        withJsonPath("caseId", is(caseId.toString())),
                        withJsonPath("caseStatus", Matchers.is("REFERRED_FOR_COURT_HEARING")
                        ))));
    }

    @Test
    public void shouldNotCreateNewDecisionTransformedEventForCaseCompletedEventIfItIsAlreadyTransformed() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();

        final JsonEnvelope caseCompleted = jsonEnvelope(caseId, decisionId, CaseCompleted.EVENT_NAME);
        final JsonObject decisionPayload = mock(JsonObject.class);
        final JsonEnvelope decisionSavedEvent = mock(JsonEnvelope.class);

        when(resultingService.getDecisionForACase(caseId)).thenReturn(decisionPayload);
        when(sjpViewStoreService.getStatus(caseId.toString())).thenReturn(Optional.of(CaseStatus.COMPLETED.toString()));
        when(decisionPayload.getString("id")).thenReturn(decisionId.toString());
        when(sjpEventStoreService.decisionTransformed(caseId.toString(), decisionId.toString()))
                .thenReturn(true);

        // when
        final Stream<JsonEnvelope> transformedEventsStream = decisionsTransformer.apply(caseCompleted);

        // then
        verify(resultingService, times(1)).getDecisionForACase(caseId);
        verifyZeroInteractions(resultingDecisionsConverter);

        List<JsonEnvelope> eventList = transformedEventsStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), is(caseCompleted));
    }

    @Test
    public void shouldCreateNewDecisionTransformedEventForTheAdjournEvent() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final String adjournTo = "2019-05-12";

        final JsonEnvelope caseAdjournEvent = jsonEnvelopeForAdjourn(caseId,
                sessionId,
                adjournTo,
                "sjp.events.case-adjourned-to-later-sjp-hearing-recorded");
        final JsonObject decisionPayload = mock(JsonObject.class);
        final JsonEnvelope decisionSavedEvent = mock(JsonEnvelope.class);

        when(resultingService.getAdjournPayloadForACase(caseId, sessionId.toString(), adjournTo)).thenReturn(decisionPayload);
        when(resultingDecisionsConverter.convert(caseAdjournEvent, decisionPayload)).thenReturn(decisionSavedEvent);

        Stream<JsonEnvelope> transformedEventsStream = decisionsTransformer.apply(caseAdjournEvent);

        verify(resultingService, times(1)).getAdjournPayloadForACase(caseId, sessionId.toString(), adjournTo);
        verify(resultingDecisionsConverter, times(1)).convert(caseAdjournEvent, decisionPayload);

        final List<JsonEnvelope> eventList = transformedEventsStream.collect(Collectors.toList());
        assertThat(eventList.get(0), is(decisionSavedEvent));
        assertThat(eventList.get(1), is(caseAdjournEvent));
    }

    @Test
    public void shouldNotCreateNewDecisionTransformedEventForAdjournEventIfItIsAlreadyTransformed() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final String adjournTo = "2019-05-12";

        final JsonEnvelope caseAdjournEvent = jsonEnvelopeForAdjourn(caseId,
                sessionId,
                adjournTo,
                "sjp.events.case-adjourned-to-later-sjp-hearing-recorded");
        final JsonObject decisionPayload = mock(JsonObject.class);
        final JsonEnvelope decisionSavedEvent = mock(JsonEnvelope.class);


        when(resultingService.getAdjournPayloadForACase(caseId, sessionId.toString(), adjournTo)).thenReturn(decisionPayload);
        when(decisionPayload.getString("decisionId")).thenReturn(decisionId.toString());
        when(sjpEventStoreService.decisionTransformed(caseId.toString(), decisionId.toString()))
                .thenReturn(true);

        // when
        final Stream<JsonEnvelope> transformedEventsStream = decisionsTransformer.apply(caseAdjournEvent);

        // then
        verify(resultingService, times(1))
                .getAdjournPayloadForACase(caseId, sessionId.toString(), adjournTo);
        verifyZeroInteractions(resultingDecisionsConverter);

        final List<JsonEnvelope> eventList = transformedEventsStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), is(caseAdjournEvent));
    }


    private JsonEnvelope jsonEnvelope(final UUID caseId,
                                      final UUID decisionId,
                                      final String eventName) {
        return createEnvelope(
                eventName,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("id", decisionId.toString())
                        .build()
        );
    }

    private JsonEnvelope jsonEnvelopeForAdjourn(final UUID caseId,
                                                final UUID sessionId,
                                                final String adjournTo,
                                                final String eventName) {
        return createEnvelope(
                eventName,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("sessionId", sessionId.toString())
                        .add("adjournedTo", adjournTo)
                        .build()
        );
    }

}