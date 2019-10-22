package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeListMatcher.listContaining;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getReferenceDecisionSaved;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getReferenceDecisionsSavedWithNoOffences;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getReferenceDecisionsSavedWithNoResults;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSJPSessionJsonObject;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.resulting.Offence;
import uk.gov.moj.cpp.sjp.domain.resulting.Prompt;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.io.StringReader;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
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
    private static final String DISMISSED_SHORT_CODE = "D";

    private static final DefaultJsonEnvelopeProvider defaultJsonEnvelopeProvider = new DefaultJsonEnvelopeProvider();
    private static final ObjectMapperProducer objectMapperProducer = new ObjectMapperProducer();

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private DecisionProcessor caseDecisionListener;

    @Mock
    private SjpService sjpService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public static final String PUBLIC_RESULTING_REFERENCED_DECISIONS_SAVED = "public.resulting.referenced-decisions-saved";

    private static Envelope emptyEnvelope = createEnvelope(PUBLIC_RESULTING_REFERENCED_DECISIONS_SAVED, Json.createObjectBuilder().build());

    private static JsonEnvelope toJsonEnvelope(final Object envelope) {
        if (envelope instanceof JsonEnvelope) {
            return (JsonEnvelope) envelope;
        } else if (envelope instanceof DefaultEnvelope) {
            try {
                final DefaultEnvelope defaultEnvelope = (DefaultEnvelope) envelope;
                final String jsonString = objectMapperProducer.objectMapper().writeValueAsString(defaultEnvelope.payload());
                final StringReader stringReader = new StringReader(jsonString);
                final JsonObject payload = Json.createReader(stringReader).readObject();
                stringReader.close();
                return defaultJsonEnvelopeProvider.envelopeFrom(defaultEnvelope.metadata(), payload);
            } catch (final JsonProcessingException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }

        } else {
            throw new IllegalArgumentException("don't know how to convert this");
        }
    }

    @Before
    public void setUp() {
        initMocks(this);
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        when(sjpService.getSessionDetails(any(), any())).thenReturn(getSJPSessionJsonObject());
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(referenceDataService.getAllResultDefinitions(any())).thenReturn(getAllResultsDefinitionJsonObject());
    }

    @Test
    public void shouldCompleteCase() {
        //given
        final UUID caseId = randomUUID();
        final ReferencedDecisionsSaved referencedDecisionsSaved =
                referenceDecisionsSaved()
                        .withCaseId(caseId)
                        .withOffences(asList(new Offence(randomUUID(), asList(new Result(randomUUID(), asList(new Prompt(randomUUID(), "value1")))))))
                        .build();

        final Envelope<ReferencedDecisionsSaved> envelope = envelop(referencedDecisionsSaved)
                .withName(PUBLIC_RESULTING_REFERENCED_DECISIONS_SAVED)
                .withMetadataFrom(emptyEnvelope);
        //when
        caseDecisionListener.referencedDecisionsSaved(envelope);
        //then
        verify(sender).send(envelopeCaptor.capture());
        final List<JsonEnvelope> allValues = standardizeJsonEnvelopes((List) envelopeCaptor.getAllValues());

        assertThat(allValues, listContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("sjp.command.complete-case"),
                        payloadIsJson(
                                withJsonPath("$.caseId", equalTo(caseId.toString()))
                        ))
                )
        );
    }

    private ReferencedDecisionsSaved.Builder referenceDecisionsSaved() {
        return ReferencedDecisionsSaved.newBuilder();
    }

    @Test
    public void shouldHandleDecisionSavedEvents() {
        assertThat(DecisionProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("referencedDecisionsSaved").thatHandles("public.resulting.referenced-decisions-saved")));
    }


    @Test
    public void shouldErrorIfNoResultsReceived() {
        try {
            caseDecisionListener.referencedDecisionsSaved(getReferenceDecisionsSavedWithNoResults());
            fail("Should throw IllegalArgumentException when no Results received");
        } catch (final IllegalArgumentException exception) {
            Assert.assertThat(exception.getMessage(), equalTo("No results for offence in ReferencedDecisionsSaved event"));
        }
    }

    @Test
    public void shouldErrorIfNoOffencesReceived() {
        try {
            caseDecisionListener.referencedDecisionsSaved(getReferenceDecisionsSavedWithNoOffences());
            fail("Should throw IllegalArgumentException when no Results received");
        } catch (final IllegalArgumentException exception) {
            Assert.assertThat(exception.getMessage(), equalTo("No offences in ReferencedDecisionsSaved event"));
        }
    }

    @Test
    public void shouldGenerateAllOffencesWithdrawnOrDismissedEvent() {
        final Envelope<ReferencedDecisionsSaved> referenceDecisionSaved = getReferenceDecisionSaved(UUID.fromString(DISMISSED_RESULT_ID), UUID.fromString(WITHDRAWN_RESULT_ID));

        caseDecisionListener.referencedDecisionsSaved(referenceDecisionSaved);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final List<JsonEnvelope> allValues = standardizeJsonEnvelopes((List) envelopeCaptor.getAllValues());

        assertThat(allValues, hasSize(2));
        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        metadata().withName("sjp.command.complete-case"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(notNullValue()))
                                )
                        )),
                jsonEnvelope(
                        metadata().withName("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", Matchers.equalTo(referenceDecisionSaved.payload().getCaseId().toString()))
                        )))
        ));
    }

    private List<JsonEnvelope> standardizeJsonEnvelopes(final List<Object> allValues) {
        return allValues.stream().map(DecisionProcessorTest::toJsonEnvelope).collect(toList());
    }

    @Test
    public void shouldNotGenerateAllOffencesWithdrawnOrDismissedEventWhenOneOffenceIsWithdrawnButAnotherIsNotWithdrawnOrDismissed() {
        final Envelope<ReferencedDecisionsSaved> referenceDecisionSaved = getReferenceDecisionSaved(UUID.fromString(WITHDRAWN_RESULT_ID), UUID.fromString(RESULT_ID));

        caseDecisionListener.referencedDecisionsSaved(referenceDecisionSaved);

        verify(sender).send(envelopeCaptor.capture());

        final List<JsonEnvelope> allValues = standardizeJsonEnvelopes((List) envelopeCaptor.getAllValues());

        assertThat(allValues, hasSize(1));
        assertThat(allValues, listContaining(
                jsonEnvelope(
                        metadata().withName("sjp.command.complete-case"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(notNullValue()))
                                )
                        ))));
    }


    @Test
    public void shouldNotGenerateAllOffencesWithdrawnOrDismissedEventWhenOneOffenceIsDismissedButAnotherIsNotWithdrawnOrDismissed() {
        final Envelope<ReferencedDecisionsSaved> referenceDecisionSaved = getReferenceDecisionSaved(UUID.fromString(DISMISSED_RESULT_ID), UUID.fromString(RESULT_ID));

        caseDecisionListener.referencedDecisionsSaved(referenceDecisionSaved);

        verify(sender).send(envelopeCaptor.capture());

        final List<JsonEnvelope> allValues = standardizeJsonEnvelopes((List) envelopeCaptor.getAllValues());

        assertThat(allValues, hasSize(1));
        assertThat(allValues, listContaining(
                jsonEnvelope(
                        metadata().withName("sjp.command.complete-case"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(notNullValue()))
                                )
                        ))));

        assertThat(allValues, not(contains(
                jsonEnvelope().withMetadataOf(
                        metadata().withName("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn")
                ))));
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


}
