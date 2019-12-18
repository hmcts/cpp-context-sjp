package uk.gov.moj.cpp.sjp.query.view.converter;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferencedDecisionSavedOffenceConverterTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private final UUID OFFENCE1_ID = randomUUID();
    private final UUID OFFENCE2_ID = randomUUID();
    private final UUID OFFENCE3_ID = randomUUID();
    private final UUID DECISION1_ID = randomUUID();
    private static final String DISMISS_RESULT_CODE = "D";
    private static final String FINE_RESULT_CODE = "FO";
    private static final String FCOMP_RESULT_CODE = "FCOMP";
    private static final String FCOST_RESULT_CODE = "FCOST";
    private static final String FVS_RESULT_CODE = "FVS";
    private static final String LSUM_RESULT_CODE = "LSUM";
    private static final String REFERRED_RESULT_CODE = "SUMRCC";

    private static final UUID LSUM_RESULT_TYPE_ID = randomUUID();
    private static final UUID FVS_RESULT_TYPE_ID = randomUUID();
    private static final UUID FCOST_RESULT_TYPE_ID = randomUUID();
    private static final UUID FINE_RESULT_TYPE_ID = randomUUID();
    private static final UUID DISMISS_RESULT_TYPE_ID = randomUUID();
    private static final UUID FCOMP_RESULT_TYPE_ID = randomUUID();
    private static final UUID REFERRED_RESULT_TYPE_ID = randomUUID();

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");

    @InjectMocks
    private ReferencedDecisionSavedOffenceConverter referencedDecisionSavedOffenceConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    @Test
    public void shouldConvertToReferencedDecisionSavedEvent() {

        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("case-results-tests/decision-saved-event.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .put("offence3Id", OFFENCE3_ID)
                                .build()));

        final List<JsonObject> resultIds = asList(
                createObjectBuilder()
                        .add("id", DISMISS_RESULT_TYPE_ID.toString())
                        .add("code", DISMISS_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", FINE_RESULT_TYPE_ID.toString())
                        .add("code", FINE_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", FCOST_RESULT_TYPE_ID.toString())
                        .add("code", FCOST_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", FCOMP_RESULT_TYPE_ID.toString())
                        .add("code", FCOMP_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", FVS_RESULT_TYPE_ID.toString())
                        .add("code", FVS_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", LSUM_RESULT_TYPE_ID.toString())
                        .add("code", LSUM_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", REFERRED_RESULT_TYPE_ID.toString())
                        .add("code", REFERRED_RESULT_CODE).build()
                );

        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        assertThat(actualPayload, hasSize(4));
        final JsonObject dismissResults = actualPayload.getJsonObject(0);
        final JsonObject fcostResults = actualPayload.getJsonObject(1);
        final JsonObject refResults = actualPayload.getJsonObject(2);
        final JsonObject fvsResults = actualPayload.getJsonObject(3);

        assertThat(dismissResults, payloadIsJson(allOf(
            withJsonPath("$.id", is(OFFENCE1_ID.toString())),
            withJsonPath("$.results[0].code", is(DISMISS_RESULT_CODE)),
            withJsonPath("$.results[0].resultTypeId", is(DISMISS_RESULT_TYPE_ID.toString())),
            withJsonPath("$.results[0].terminalEntries", empty())
        )));
       assertThat(fcostResults, payloadIsJson(allOf(
            withJsonPath("$.id", is(OFFENCE2_ID.toString())),
            withJsonPath("$.results[0].code", is(FINE_RESULT_CODE)),
            withJsonPath("$.results[0].resultTypeId", is(FINE_RESULT_TYPE_ID.toString())),
            withJsonPath("$.results[0].terminalEntries[0].index", is(1)),
            withJsonPath("$.results[0].terminalEntries[0].value", is("40")),
            withJsonPath("$.results[1].code", is(FCOMP_RESULT_CODE)),
            withJsonPath("$.results[1].resultTypeId", is(FCOMP_RESULT_TYPE_ID.toString())),
            withJsonPath("$.results[1].terminalEntries[0].index", is(1)),
            withJsonPath("$.results[1].terminalEntries[0].value", is("15.3"))
        )));
        assertThat(fvsResults, payloadIsJson(allOf(
                withJsonPath("$.results[0].code", is(FCOST_RESULT_CODE)),
                withJsonPath("$.results[0].resultTypeId", is(FCOST_RESULT_TYPE_ID.toString())),
                withJsonPath("$.results[0].terminalEntries[0].index", is(1)),
                withJsonPath("$.results[0].terminalEntries[0].value", is("20.1")),
                withJsonPath("$.results[1].code", is(FVS_RESULT_CODE)),
                withJsonPath("$.results[1].resultTypeId", is(FVS_RESULT_TYPE_ID.toString())),
                withJsonPath("$.results[1].terminalEntries[0].index", is(1)),
                withJsonPath("$.results[1].terminalEntries[0].value", is("32")),
                withJsonPath("$.results[2].code", is(LSUM_RESULT_CODE)),
                withJsonPath("$.results[2].resultTypeId", is(LSUM_RESULT_TYPE_ID.toString())),
                withJsonPath("$.results[2].terminalEntries[0].index", is(5)),
                withJsonPath("$.results[2].terminalEntries[0].value", is("107.4")),
                withJsonPath("$.results[2].terminalEntries[1].index", is(6)),
                withJsonPath("$.results[2].terminalEntries[1].value", is("Lump sum within 14 days"))
        )));
        assertThat(refResults, payloadIsJson(allOf(
                withJsonPath("$.results[0].code", is(REFERRED_RESULT_CODE)),
                withJsonPath("$.results[0].resultTypeId", is(REFERRED_RESULT_TYPE_ID.toString()))
        )));
    }
}
