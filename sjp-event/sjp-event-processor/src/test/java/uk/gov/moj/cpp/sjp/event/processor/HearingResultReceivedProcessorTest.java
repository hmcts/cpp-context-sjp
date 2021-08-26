package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN;
import static uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted.publicHearingResulted;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AACA;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AACD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AASA;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AASD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.ACSD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.APA;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.ASV;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AW;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.G;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.RFSD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.ROPENED;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.STDEC;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.WDRN;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_CONVICTION;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_SENTENCE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_SENTENCE_AND_CONVICTION;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPLICATION_TO_REOPEN_CASE;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultReceivedProcessorTest {

    @Mock
    protected Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private HearingResultReceivedProcessor hearingResultReceivedProcessor;

    private JsonObjectToObjectConverter jsonObjectConverter;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    final UUID caseId = randomUUID();

    @Before
    public void setUp() {
        objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
        jsonObjectConverter = new JsonObjectToObjectConverter();
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        setField(this.objectToJsonObjectConverter, "mapper", objectMapper);
        setField(this.jsonObjectConverter, "objectMapper", objectMapper);
        setField(this.hearingResultReceivedProcessor, "jsonObjectConverter", jsonObjectConverter);
    }

    private JsonEnvelope populateHearing(String applicationType, UUID caseId, boolean isSJP, String resultDefinitionId) {

        JudicialResult judicialResult = new JudicialResult.Builder()
                .withJudicialResultTypeId(UUID.fromString(resultDefinitionId))
                .build();
        CourtApplicationCase courtApplicationCase = new CourtApplicationCase.Builder()
                .withIsSJP(isSJP)
                .withProsecutionCaseId(caseId)
                .build();
        CourtApplicationType courtApplicationType = new CourtApplicationType.Builder()
                .withType(applicationType)

                .build();
        CourtApplication courtApplication = courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationType)
                .withJudicialResults(Arrays.asList(judicialResult))
                .withCourtApplicationCases(Arrays.asList(courtApplicationCase))
                .build();

        final PublicHearingResulted publicHearingResulted = publicHearingResulted()
                .withHearing(hearing()
                        .withCourtApplications(singletonList(courtApplication))
                        .withIsSJPHearing(false)
                        .build())
                .build();


        final JsonEnvelope hearingJsonEnvelope = envelopeFrom(metadataWithRandomUUID(HearingResultReceivedProcessor.PUBLIC_HEARING_RESULTED),
                objectToJsonObjectConverter.convert(publicHearingResulted));
        return hearingJsonEnvelope;
    }


    private void runTest(String applicationType, String resultDefinitionId, ApplicationStatus applicationStatus) {
        JsonEnvelope hearingJsonEnvelope = populateHearing(applicationType, caseId, true, resultDefinitionId);
        hearingResultReceivedProcessor.hearingResultReceived(hearingJsonEnvelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), equalTo("sjp.command.update-cc-case-application-status"));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.applicationStatus", equalTo(applicationStatus.toString())))));
    }

    @Test
    public void shouldHandleHearingResultReceivedWithJudicialResultsMatch() throws IOException {


        // Scenario 1
        runTest(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType(), G.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        runTest(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType(), STDEC.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);

        // Scenario 2
        runTest(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType(), RFSD.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_REFUSED);
        // Scenario 3
        runTest(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType(), WDRN.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_WITHDRAWN);

        // Scenario 4
        runTest(APPLICATION_TO_REOPEN_CASE.getApplicationType(), G.getResultId(), ApplicationStatus.REOPENING_GRANTED);
        runTest(APPLICATION_TO_REOPEN_CASE.getApplicationType(), ROPENED.getResultId(), ApplicationStatus.REOPENING_GRANTED);

        // Scenario 5
        runTest(APPLICATION_TO_REOPEN_CASE.getApplicationType(), RFSD.getResultId(), ApplicationStatus.REOPENING_REFUSED);

        // Scenario 6
        runTest(APPLICATION_TO_REOPEN_CASE.getApplicationType(), WDRN.getResultId(), ApplicationStatus.REOPENING_WITHDRAWN);

        // Scenario 7
        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);

        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);

        // Scenario 8
        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);

        // Scenario 9
        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);

        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);

        // Scenario 10
        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);

        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);

        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);

        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);

        // Scenario 11
        runTest(APPEAL_AGAINST_CONVICTION.getApplicationType(), APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);
        runTest(APPEAL_AGAINST_SENTENCE.getApplicationType(), APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);
        runTest(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType(), APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);

    }


    @Test
    public void shouldHandleHearingResultReceivedWithJudicialResultsNotMatch() throws IOException {

        JsonEnvelope hearingJsonEnvelope = populateHearing("SOME RANDOM STATUS", caseId, true, randomUUID().toString());
        hearingResultReceivedProcessor.hearingResultReceived(hearingJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), equalTo("sjp.command.update-cc-case-application-status"));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.applicationStatus", equalTo(APPLICATION_STATUS_NOT_KNOWN.toString())))));

    }

    @Test
    public void shouldHandleHearingResultReceivedWithJudicialResultsMatchAndNonSJP() throws IOException {

        JsonEnvelope hearingJsonEnvelope = populateHearing(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType(), caseId, false, STDEC.getResultId());
        hearingResultReceivedProcessor.hearingResultReceived(hearingJsonEnvelope);

        verify(sender, never()).send(anyObject());
    }

    @Test
    public void shouldHandleHearingResultReceivedWithApplicationTypeNotInCommonTypesJudicialResultsMatchAndNonSJP() throws IOException {

        JsonEnvelope hearingJsonEnvelope = populateHearing("DUMMY", caseId, false, STDEC.getResultId());
        hearingResultReceivedProcessor.hearingResultReceived(hearingJsonEnvelope);

        verify(sender, never()).send(anyObject());
    }

    @Test
    public void shouldHandleHearingResultReceivedWhenCourtApplicationsIsNotPresent() throws IOException {

        JsonObjectBuilder hearingJsonBuilder = Json.createObjectBuilder();
        hearingJsonBuilder.add("id", randomUUID().toString());
        JsonObjectBuilder hearingEnvelopeJsonBuilder = Json.createObjectBuilder();
        hearingEnvelopeJsonBuilder.add("hearing", hearingJsonBuilder);

        final JsonEnvelope hearingJsonEnvelope = envelopeFrom(metadataWithRandomUUID(HearingResultReceivedProcessor.PUBLIC_HEARING_RESULTED),
                hearingEnvelopeJsonBuilder);
        hearingResultReceivedProcessor.hearingResultReceived(hearingJsonEnvelope);
        verify(sender, never()).send(anyObject());
    }

    @Test
    public void shouldHandleHearingResultReceivedWhenCourtApplicationIsNotPresent() throws IOException {

        JsonObjectBuilder courtApplicationJsonObjectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder courtApplications = Json.createArrayBuilder();
        courtApplicationJsonObjectBuilder.add("courtApplications", courtApplications);
        JsonObjectBuilder hearingJsonBuilder = Json.createObjectBuilder();
        hearingJsonBuilder.add("hearing", courtApplicationJsonObjectBuilder);

        final JsonEnvelope hearingJsonEnvelope = envelopeFrom(metadataWithRandomUUID(HearingResultReceivedProcessor.PUBLIC_HEARING_RESULTED),
                hearingJsonBuilder.build());

        hearingResultReceivedProcessor.hearingResultReceived(hearingJsonEnvelope);
        verify(sender, never()).send(anyObject());
    }

    @Test
    public void shouldHandleCaseReceivedEvent() {
        assertThat(CaseReceivedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleCaseReceivedEvent").thatHandles(CaseReceived.EVENT_NAME)));
    }


}
