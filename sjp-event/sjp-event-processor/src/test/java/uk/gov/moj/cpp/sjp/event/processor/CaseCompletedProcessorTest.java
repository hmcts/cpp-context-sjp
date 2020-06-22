package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;

import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseCompletedProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION1_ID = randomUUID();
    private static final UUID RESULT_DEFINITION_ID = randomUUID();
    private static final ZonedDateTime DECISION1_SAVED_AT = ZonedDateTime.now();
    private static final UUID OFFENCE1_ID = randomUUID();
    private static final String CASE_RESULTS = "sjp.query.case-results";
    private static final String CASE_COMPLETED = "sjp.events.case-completed";

    @InjectMocks
    private CaseCompletedProcessor caseCompletedProcessor;

    @Mock
    private Requester requester;

    @Mock
    private Sender sender;

    @Mock
    private SjpService sjpService;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> jsonEnvelopeCaptor;

    @Mock
    private ResultingToResultsConverter resultingToResultsConverter;
    @Mock
    private PublicSjpResulted publicSjpResulted;

    @Before
    public void setUp() {
        initMocks(this);
        final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(caseCompletedProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
        when(resultingToResultsConverter.convert(any(), any(), any(), any())).thenReturn(publicSjpResulted);
    }

    @Test
    public void shouldHandleCaseCompletedEvent() {
        assertThat(CaseCompletedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleCaseCompleted").thatHandles("sjp.events.case-completed")));
    }

    @Test
    public void shouldInvokeRequestDeleteDocs() {
        final JsonObject payload = createObjectBuilder().add("caseId", CASE_ID.toString()).add("sessionIds", createArrayBuilder().add(randomUUID().toString())).build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), payload);
        final JsonObject responsePayload = createResponsePayload();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(requester.request(any())).thenReturn(responseEnvelope);

        caseCompletedProcessor.handleCaseCompleted(envelope);

        verify(sender, times(2)).send(jsonEnvelopeCaptor.capture());

        final List<Envelope<JsonValue>> allSenderEnvelopes = jsonEnvelopeCaptor.getAllValues();
        final Envelope<JsonValue> requestDeleteDocs = allSenderEnvelopes.get(0);
        assertThat(requestDeleteDocs.payload(),
                payloadIsJson(allOf(withJsonPath("caseId", is(CASE_ID.toString())))));
        assertThat(requestDeleteDocs.metadata().name(), is("sjp.command.request-delete-docs"));

        final Envelope<JsonValue> caseResultedEnvelope = allSenderEnvelopes.get(1);
        assertThat((caseResultedEnvelope.metadata().name()), is("public.sjp.case-resulted"));
        assertThat(caseResultedEnvelope.payload(), is(publicSjpResulted));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldErrorIfNoOffencesReceived() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayloadForNoOffences();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(requester.request(any())).thenReturn(responseEnvelope);
        caseCompletedProcessor.handleCaseCompleted(envelope);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldErrorIfNoResultsReceived() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayloadForNoResults();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(requester.request(any())).thenReturn(responseEnvelope);
        caseCompletedProcessor.handleCaseCompleted(envelope);
    }

    private JsonObject createResponsePayload() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("accountDivisionCode", 77)
                .add("enforcingCourtCode", 828)
                .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()
                        .add("sjpSessionId", SESSION1_ID.toString())
                        .add("resultedOn", DECISION1_SAVED_AT.toString())
                        .add("offences", createArrayBuilder().add(createObjectBuilder()
                                .add("id", OFFENCE1_ID.toString())
                                .add("verdict", "FOUND_NOT_GUILTY")
                                .add("results", createArrayBuilder().add(createObjectBuilder()
                                        .add("resultDefinitionId", RESULT_DEFINITION_ID.toString())
                                        .add("prompts", createArrayBuilder())))))))
                .build();

    }

    private JsonObject createResponsePayloadForNoResults() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("accountDivisionCode", 77)
                .add("enforcingCourtCode", 828)
                .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()
                        .add("sjpSessionId", SESSION1_ID.toString())
                        .add("resultedOn", DECISION1_SAVED_AT.toString())
                        .add("offences", createArrayBuilder().add(createObjectBuilder()
                                .add("id", OFFENCE1_ID.toString())
                                .add("verdict", "FOUND_NOT_GUILTY")
                                .add("results", createArrayBuilder().build())))))
                .build();

    }

    private JsonObject createResponsePayloadForNoOffences() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("accountDivisionCode", 77)
                .add("enforcingCourtCode", 828)
                .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()
                        .add("sjpSessionId", SESSION1_ID.toString())
                        .add("resultedOn", DECISION1_SAVED_AT.toString())
                        .add("offences", createArrayBuilder().build())))
                .build();

    }

}
