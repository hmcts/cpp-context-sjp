package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.json.schemas.domains.sjp.results.BaseSessionStructure.baseSessionStructure;
import static uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted.publicSjpResulted;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getReferenceDecisionSaved;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSJPSessionJsonObject;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSjpSessionId;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.List;
import java.util.UUID;

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
    private ResultingToResultsConverter converter;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setUp() {
        initMocks(this);
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCompleteCase() {
        final Envelope<ReferencedDecisionsSaved> referenceDecisionSaved = getReferenceDecisionSaved();
        final ReferencedDecisionsSaved referencedDecisionsSaved = referenceDecisionSaved.payload();
        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved",
                createObjectBuilder()
                        .add("caseId", referencedDecisionsSaved.getCaseId().toString())
                        .add("resultedOn", now().toString())
                        .add("sjpSessionId", randomUUID().toString())
                        .build());
        when(converter.convert(any(), any(), any(), any())).thenReturn(publicSjpResulted().withSession(baseSessionStructure().withSessionId(getSjpSessionId()).build()).build());


        caseDecisionListener.referencedDecisionsSaved(referenceDecisionSaved);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().withName("sjp.command.complete-case"),
                        payloadIsJson(
                                withJsonPath("$.caseId", Matchers.equalTo(referenceDecisionSaved.payload().getCaseId().toString()))
                        ))));
    }

    @Test
    public void shouldHandleDecisionSavedEvents() {
        Assert.assertThat(DecisionProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("referencedDecisionsSaved").thatHandles("public.resulting.referenced-decisions-saved")));
    }

    @Test
    public void shouldRaiseSJPCaseResultedEvent() {
        final Envelope<ReferencedDecisionsSaved> referenceDecisionSaved = getReferenceDecisionSaved();
        final ReferencedDecisionsSaved referencedDecisionsSaved = referenceDecisionSaved.payload();

        when(sjpService.getSessionDetails(any(), any())).thenReturn(getSJPSessionJsonObject());
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(converter.convert(any(), any(), any(), any())).thenReturn(publicSjpResulted().withSession(baseSessionStructure().withSessionId(getSjpSessionId()).build()).build());

        caseDecisionListener.referencedDecisionsSaved(referenceDecisionSaved);

        verify(sjpService).getSessionDetails(any(), any());
        verify(sjpService).getCaseDetails(any(), any());
        verify(converter).convert(any(), any(), any(), any());
        verify(sender, times(2)).send(envelopeCaptor.capture());
        verifyCaseStateService(referenceDecisionSaved, referencedDecisionsSaved.getCaseId());

        final List<JsonEnvelope> allValues = convertStreamToEventList(envelopeCaptor.getAllValues());
        assertThat(allValues, hasSize(2));
        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        metadata().withName("sjp.command.complete-case"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(notNullValue()))
                                )
                        )),
                jsonEnvelope(
                        metadata().withName("public.sjp.case-resulted"),
                        payloadIsJson(allOf(
                                withJsonPath("$.session.sessionId", is(getSjpSessionId().toString())))
                        ))));
    }

    private List<JsonEnvelope> convertStreamToEventList(final List<JsonEnvelope> listOfStreams) {
        return listOfStreams.stream().collect(toList());
    }

    private void verifyCaseStateService(final Envelope<ReferencedDecisionsSaved> event, final UUID caseId) {
        verify(caseStateService).caseCompleted(caseId, event.metadata());
    }
}