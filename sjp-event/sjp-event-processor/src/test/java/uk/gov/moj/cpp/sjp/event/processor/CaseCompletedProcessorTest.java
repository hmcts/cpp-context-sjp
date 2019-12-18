package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
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


    @InjectMocks
    private CaseCompletedProcessor caseCompletedProcessor;

    @Mock
    private Requester requester;

    @Mock
    private Sender sender;

    @Mock
    private SjpService sjpService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;


    private static final DefaultJsonEnvelopeProvider defaultJsonEnvelopeProvider = new DefaultJsonEnvelopeProvider();
    private static final ObjectMapperProducer objectMapperProducer = new ObjectMapperProducer();

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION1_ID = randomUUID();
    private static final UUID RESULT_DEFINITION_ID = randomUUID();
    private static final ZonedDateTime DECISION1_SAVED_AT = ZonedDateTime.now();
    private static final UUID OFFENCE1_ID = randomUUID();
    private static final UUID OFFENCE2_ID = randomUUID();

    private static final String FIELD_ID = "id";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_WELSH_LABEL = "welshLabel";
    private static final String FIELD_IS_AVAILABLE_FOR_COURT_EXTRACT = "isAvailableForCourtExtract";
    private static final String FIELD_SHORT_CODE = "shortCode";
    private static final String FIELD_LEVEL = "level";
    private static final String FIELD_RANK = "rank";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_USER_GROUPS = "userGroups";
    private static final String PLACEHOLDER = "PLACEHOLDER";
    private static final String RESULT_ID = "b786ce8a-ce7a-4fa1-94ce-a3d9777574e4";
    private static final String WITHDRAWN_RESULT_ID = "6feb0f2e-8d1e-40c7-af2c-05b28c69e5fc";
    private static final String DISMISSED_RESULT_ID = "14d66587-8fbe-424f-a369-b1144f1684e3";
    private static final String WITHDRAWN_SHORT_CODE = "WDRNNOT";
    private static final String DISMISSED_SHORT_CODE = "DISM";

    private static final String CASE_RESULTS = "sjp.query.case-results";
    private static final String CASE_COMPLETED = "sjp.events.case-completed";

    @Before
    public void setUp() {
        initMocks(this);
        final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(caseCompletedProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
    }

    @Test
    public void shouldHandleCaseCompletedEvent() {
        assertThat(CaseCompletedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleCaseCompleted").thatHandles("sjp.events.case-completed")));
    }

    @Test
    public void shouldInvokeQueryService() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayload();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(referenceDataService.getAllResultDefinitions(any(), any())).thenReturn(getAllResultsDefinitionJsonObject());

        when(requester.request(any())).thenReturn(responseEnvelope);

        caseCompletedProcessor.handleCaseCompleted(envelope);

        verify(requester).request(jsonEnvelopeCaptor.capture());

        assertThat(jsonEnvelopeCaptor.getValue().payload(),
                payloadIsJson(allOf(withJsonPath("caseId", is(CASE_ID.toString())))));
    }

    @Test
    public void shouldErrorIfNoResultsReceived() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayloadForNoResults();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(referenceDataService.getAllResultDefinitions(any(), any())).thenReturn(getAllResultsDefinitionJsonObject());

        when(requester.request(any())).thenReturn(responseEnvelope);

        try {
            caseCompletedProcessor.handleCaseCompleted(envelope);
            fail("Should throw IllegalArgumentException when no Results received");
        } catch (final IllegalArgumentException exception) {
            Assert.assertThat(exception.getMessage(), equalTo("No results for offence in ReferencedDecisionsSaved event"));
        }
    }

    @Test
    public void shouldErrorIfNoOffencesReceived() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayloadForNoOffences();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(referenceDataService.getAllResultDefinitions(any(), any())).thenReturn(getAllResultsDefinitionJsonObject());
        when(requester.request(any())).thenReturn(responseEnvelope);

        try {
            caseCompletedProcessor.handleCaseCompleted(envelope);
            fail("Should throw IllegalArgumentException when no Results received");
        } catch (final IllegalArgumentException exception) {
            Assert.assertThat(exception.getMessage(), equalTo("No offences in ReferencedDecisionsSaved event"));
        }
    }

    @Test
    public void shouldGenerateAllOffencesWithdrawnOrDismissedEvent() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayloadForDismiss();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(referenceDataService.getAllResultDefinitions(any(), any())).thenReturn(getAllResultsDefinitionJsonObject());
        when(requester.request(any())).thenReturn(responseEnvelope);

        caseCompletedProcessor.handleCaseCompleted(envelope);

        verify(sender, times(1)).send(jsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> allValues = standardizeJsonEnvelopes((List) jsonEnvelopeCaptor.getAllValues());

        MatcherAssert.assertThat(allValues, hasSize(1));
        MatcherAssert.assertThat(allValues.get(0), is(
                jsonEnvelope(
                        metadata().withName("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn"),
                        payloadIsJson(Matchers.allOf(
                                withJsonPath("$.caseId", Matchers.equalTo(CASE_ID.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotGenerateAllOffencesWithdrawnOrDismissedEventWhenOneOffenceIsWithdrawnButAnotherIsNotWithdrawnOrDismissed() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayloadForOneWithdrawAndOtherNotWithDrawOrDismiss();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(referenceDataService.getAllResultDefinitions(any(), any())).thenReturn(getAllResultsDefinitionJsonObject());
        when(requester.request(any())).thenReturn(responseEnvelope);

        caseCompletedProcessor.handleCaseCompleted(envelope);
        verify(sender, times(0)).send(jsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldNotGenerateAllOffencesWithdrawnOrDismissedEventWhenOneOffenceIsDismissButAnotherIsNotWithdrawnOrDismissed() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonObject responsePayload = createResponsePayloadForOneDismissAndOtherNotWithDrawOrDismiss();
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_RESULTS), responsePayload);

        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(referenceDataService.getAllResultDefinitions(any(), any())).thenReturn(getAllResultsDefinitionJsonObject());
        when(requester.request(any())).thenReturn(responseEnvelope);

        caseCompletedProcessor.handleCaseCompleted(envelope);
        verify(sender, times(0)).send(jsonEnvelopeCaptor.capture());
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

    private JsonObject createResponsePayloadForDismiss() {
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
                                        .add("resultDefinitionId", DISMISSED_RESULT_ID.toString())
                                        .add("prompts", createArrayBuilder())))))))
                .build();

    }

    private JsonObject createResponsePayloadForWithdraw() {
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
                                        .add("resultDefinitionId", WITHDRAWN_RESULT_ID.toString())
                                        .add("prompts", createArrayBuilder())))))))
                .build();

    }

    private JsonObject createResponsePayloadForOneWithdrawAndOtherNotWithDrawOrDismiss() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("accountDivisionCode", 77)
                .add("enforcingCourtCode", 828)
                .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()
                        .add("sjpSessionId", SESSION1_ID.toString())
                        .add("resultedOn", DECISION1_SAVED_AT.toString())
                        .add("offences", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", OFFENCE1_ID.toString())
                                        .add("verdict", "FOUND_NOT_GUILTY")
                                        .add("results", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("resultDefinitionId", WITHDRAWN_RESULT_ID.toString())
                                                        .add("prompts", createArrayBuilder()))))
                                .add(createObjectBuilder()
                                        .add("id", OFFENCE2_ID.toString())
                                        .add("verdict", "FOUND_GUILTY")
                                        .add("results", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("resultDefinitionId", RESULT_DEFINITION_ID.toString())
                                                        .add("prompts", createArrayBuilder())))))))
                .build();
    }

    private JsonObject createResponsePayloadForOneDismissAndOtherNotWithDrawOrDismiss() {
        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("accountDivisionCode", 77)
                .add("enforcingCourtCode", 828)
                .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()
                        .add("sjpSessionId", SESSION1_ID.toString())
                        .add("resultedOn", DECISION1_SAVED_AT.toString())
                        .add("offences", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", OFFENCE1_ID.toString())
                                        .add("verdict", "FOUND_NOT_GUILTY")
                                        .add("results", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("resultDefinitionId", DISMISSED_RESULT_ID.toString())
                                                        .add("prompts", createArrayBuilder()))))
                                .add(createObjectBuilder()
                                        .add("id", OFFENCE2_ID.toString())
                                        .add("verdict", "FOUND_GUILTY")
                                        .add("results", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("resultDefinitionId", RESULT_DEFINITION_ID.toString())
                                                        .add("prompts", createArrayBuilder())))))))
                .build();
    }

    private final JsonArray getAllResultsDefinitionJsonObject() {

        final JsonObject withdrawnResultDefinitionJsonObject = createObjectBuilder()
                .add(FIELD_ID, WITHDRAWN_RESULT_ID)
                .add(FIELD_VERSION, PLACEHOLDER)
                .add(FIELD_LABEL, PLACEHOLDER)
                .add(FIELD_WELSH_LABEL, PLACEHOLDER)
                .add(FIELD_SHORT_CODE, WITHDRAWN_SHORT_CODE)
                .add(FIELD_LEVEL, PLACEHOLDER)
                .add(FIELD_USER_GROUPS, PLACEHOLDER)
                .add(FIELD_RANK, PLACEHOLDER)
                .add(FIELD_START_DATE, PLACEHOLDER)
                .add(FIELD_IS_AVAILABLE_FOR_COURT_EXTRACT, PLACEHOLDER)
                .build();

        final JsonObject dismissedResultDefinitionJsonObject = createObjectBuilder()
                .add(FIELD_ID, DISMISSED_RESULT_ID)
                .add(FIELD_VERSION, PLACEHOLDER)
                .add(FIELD_LABEL, PLACEHOLDER)
                .add(FIELD_WELSH_LABEL, PLACEHOLDER)
                .add(FIELD_SHORT_CODE, DISMISSED_SHORT_CODE)
                .add(FIELD_LEVEL, PLACEHOLDER)
                .add(FIELD_USER_GROUPS, PLACEHOLDER)
                .add(FIELD_RANK, PLACEHOLDER)
                .add(FIELD_START_DATE, PLACEHOLDER)
                .add(FIELD_IS_AVAILABLE_FOR_COURT_EXTRACT, PLACEHOLDER)
                .build();

        return createArrayBuilder()
                .add(withdrawnResultDefinitionJsonObject)
                .add(dismissedResultDefinitionJsonObject)
                .build();
    }

    private List<JsonEnvelope> standardizeJsonEnvelopes(final List<Object> allValues) {
        return allValues.stream().map(CaseCompletedProcessorTest::toJsonEnvelope).collect(toList());
    }

    private static JsonEnvelope toJsonEnvelope(final Object envelope) {
        if (envelope instanceof JsonEnvelope) {
            return (JsonEnvelope) envelope;
        } else if (envelope instanceof DefaultEnvelope) {
            try {
                final DefaultEnvelope defaultEnvelope = (DefaultEnvelope) envelope;
                final String jsonString = objectMapperProducer.objectMapper().writeValueAsString(defaultEnvelope.payload());
                final StringReader stringReader = new StringReader(jsonString);
                final JsonObject payload = createReader(stringReader).readObject();
                stringReader.close();
                return defaultJsonEnvelopeProvider.envelopeFrom(defaultEnvelope.metadata(), payload);
            } catch (final JsonProcessingException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }

        } else {
            throw new IllegalArgumentException("don't know how to convert this");
        }
    }

}
