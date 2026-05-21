package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.ResultingToResultsConverterHelper.buildCaseDetails;
import static uk.gov.moj.cpp.sjp.event.processor.utils.FileUtil.getFileContentAsJson;

import uk.gov.justice.json.schemas.domains.sjp.CaseApplication;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CaseCompletedProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION1_ID = randomUUID();
    private static final UUID RESULT_DEFINITION_ID = randomUUID();
    private static final ZonedDateTime DECISION1_SAVED_AT = ZonedDateTime.now();
    private static final UUID OFFENCE1_ID = randomUUID();
    private static final String CASE_RESULTS = "sjp.query.case-results";
    private static final String COMMON_CASE_APPLICATION = "sjp.query.common-case-application";
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
    private ArgumentCaptor<Envelope<JsonValue>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PublicSjpResulted>> publicSjpResultedEnvelope;

    @Mock
    private PublicSjpResulted publicSjpResulted;

    private JsonObjectToObjectConverter jsonObjectToObjectConverter =null;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(caseCompletedProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
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


        caseCompletedProcessor.handleCaseCompleted(envelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonValue>> allSenderEnvelopes = envelopeArgumentCaptor.getAllValues();
        final Envelope<JsonValue> requestDeleteDocs = allSenderEnvelopes.get(0);
        assertThat(requestDeleteDocs.payload(),
                payloadIsJson(allOf(withJsonPath("caseId", is(CASE_ID.toString())))));
        assertThat(requestDeleteDocs.metadata().name(), is("sjp.command.request-delete-docs"));


    }

    @Test
    public void shouldRaisePublicSjpResultedEventWhenApplicationDecisionIsPresent() {
        // given
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());
        final JsonEnvelope caseResultsResponse = envelopeFrom(metadataWithRandomUUID(CASE_RESULTS),
                getFileContentAsJson("CaseCompletedProcessorTest/sjp.query.case-results.json", new HashMap<>()));

        final JsonEnvelope commonCaseApplicationResponse = envelopeFrom(metadataWithRandomUUID(COMMON_CASE_APPLICATION),
                getFileContentAsJson("CaseCompletedProcessorTest/sjp.query.common-case-application.json", new HashMap<>()));
        final CaseDetails caseDetails = mock(CaseDetails.class);
        final CaseApplication caseApplication = mock(CaseApplication.class);
        final QueryApplicationDecision queryApplicationDecision = mock(QueryApplicationDecision.class);
        final Session session = mock(uk.gov.justice.json.schemas.domains.sjp.queries.Session.class);
        final UUID sessionId = UUID.randomUUID();

        final JsonObject sessionJsonObject = mock(JsonObject.class);

        // when
        caseCompletedProcessor.handleCaseCompleted(envelope);

        // then 2 public events are emitted
        // more asserts needed here
        verify(sender,times(1)).send(publicSjpResultedEnvelope.capture());

        final List<Envelope<PublicSjpResulted>> envelopes = publicSjpResultedEnvelope.getAllValues();
        assertThat(envelopes.get(0).metadata().name(), is("sjp.command.request-delete-docs"));

    }

    @Test
    public void shouldRaiseTwoPublicSjpResultedEventsWhenTwoCaseDecisionsAreMade() {
        // given
        // case completed envelope
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED), createObjectBuilder().add("caseId", CASE_ID.toString()).build());

        // case results
        final JsonEnvelope caseResultsResponse = envelopeFrom(metadataWithRandomUUID(CASE_RESULTS),
                getFileContentAsJson("CaseCompletedProcessorTest/sjp.query.case-results-it.json", new HashMap<>()));

        // query case
        final JsonObject caseDetailsJson = getFileContentAsJson("CaseCompletedProcessorTest/sjp.query.case-it.json", new HashMap<>());
        final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(caseDetailsJson, CaseDetails.class);


        final JsonObject sessionJsonObject = mock(JsonObject.class);

        // when
        caseCompletedProcessor.handleCaseCompleted(envelope);

        // then
        verify(sender,times(1)).send(publicSjpResultedEnvelope.capture());

        final List<Envelope<PublicSjpResulted>> envelopes = publicSjpResultedEnvelope.getAllValues();
        assertThat(envelopes.get(0).metadata().name(), is("sjp.command.request-delete-docs"));

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
