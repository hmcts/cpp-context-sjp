package uk.gov.moj.cpp.sjp.query.view.converter;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJsonArray;

@RunWith(MockitoJUnitRunner.class)
public class ReferencedDecisionSavedOffenceConverterTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final String DISMISS_RESULT_CODE = "D";
    private static final String FINE_RESULT_CODE = "FO";
    private static final String FCOMP_RESULT_CODE = "FCOMP";
    private static final String FCOST_RESULT_CODE = "FCOST";
    private static final String NCOSTS_RESULT_CODE = "NCOSTS";
    private static final String FVS_RESULT_CODE = "FVS";
    private static final String LSUM_RESULT_CODE = "LSUM";
    private static final String BACK_DUTY_RESULT_CODE = "FVEBD";
    private static final String EXCISE_PENALTY_RESULT_CODE = "EXPEN";
    private static final String ABSOLUTE_DISCHARGE_RESULT_CODE = "AD";
    private static final String REFERRED_RESULT_CODE = "SUMRCC";
    private static final UUID LSUM_RESULT_TYPE_ID = UUID.fromString("49a84ca7-d5af-4292-b39e-777a478ca182");
    private static final UUID FVS_RESULT_TYPE_ID = UUID.fromString("3f464288-fb5b-4e6e-adb0-7133bc562cda");
    private static final UUID FCOST_RESULT_TYPE_ID = UUID.fromString("80673cfd-f40b-4088-9aa0-d192fb877e16");
    private static final UUID NCOSTS_RESULT_TYPE_ID = UUID.fromString("84b90ddf-38c8-4e27-83db-112348cf1284");
    private static final UUID FINE_RESULT_TYPE_ID = UUID.fromString("c054d9fa-8595-4b0b-81fc-5bfdc1d7266b");
    private static final UUID DISMISS_RESULT_TYPE_ID = UUID.fromString("06fd8e99-83af-422a-b0a9-db37188bfa74");
    private static final UUID FCOMP_RESULT_TYPE_ID = UUID.fromString("9383d0ff-15a7-47ac-a471-97fce0135e94");
    private static final UUID BACK_DUTY_RESULT_TYPE_ID = UUID.fromString("ff640415-e485-4490-bfba-2b2b3c567248");
    private static final UUID EXCISE_PENALTY_RESULT_TYPE_ID = UUID.fromString("ce276903-632c-4796-921b-23035fa934cb");
    private static final UUID ABSOLUTE_DISCHARGE_RESULT_TYPE_ID = UUID.fromString("ba276903-632c-4796-921b-23035fa934cb");
    private static final UUID REFERRED_RESULT_TYPE_ID = UUID.fromString("aa276903-632c-4796-921b-23035fa934ca");
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    private final UUID OFFENCE1_ID = randomUUID();
    private final UUID OFFENCE2_ID = randomUUID();
    private final UUID OFFENCE3_ID = randomUUID();
    private final UUID DECISION1_ID = randomUUID();
    @InjectMocks
    private ReferencedDecisionSavedOffenceConverter referencedDecisionSavedOffenceConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    private List<JsonObject> resultIds;

    @Before
    public void setup() {
        resultIds = asList(
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
                        .add("id", NCOSTS_RESULT_TYPE_ID.toString())
                        .add("code", NCOSTS_RESULT_CODE).build(),
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
                        .add("id", BACK_DUTY_RESULT_TYPE_ID.toString())
                        .add("code", BACK_DUTY_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", EXCISE_PENALTY_RESULT_TYPE_ID.toString())
                        .add("code", EXCISE_PENALTY_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", ABSOLUTE_DISCHARGE_RESULT_TYPE_ID.toString())
                        .add("code", ABSOLUTE_DISCHARGE_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", REFERRED_RESULT_TYPE_ID.toString())
                        .add("code", REFERRED_RESULT_CODE).build()
        );
    }

    @Test
    public void shouldConvertFinancialPenalty() {

        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.fine.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expectedOffenceDecisions = getFileContentAsJsonArray("converter/decision-saved-event.fine.output.json");

        assertEquals(expectedOffenceDecisions.toString(), actualPayload.toString(), getCustomComparator());
    }

    @Test
    public void shouldConvertAbsoluteDischargeDVLA() {

        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.discharge.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expectedOffenceDecisions = getFileContentAsJsonArray("converter/decision-saved-event.discharge.output.json");

        assertEquals(expectedOffenceDecisions.toString(), actualPayload.toString(), getCustomComparator());
    }

    @Test
    public void shouldConvertFinancialPenaltyForDVLAWithNoVictimSurcharge() {

        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.novictimsurcharge.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expectedOffenceDecisions = getFileContentAsJsonArray("converter/decision-saved-event.novictimsurcharge.output.json");

        assertEquals(expectedOffenceDecisions.toString(), actualPayload.toString(), getCustomComparator());

    }

    @Test
    public void shouldConvertFinancialPenaltyWithZeroCostsAndReasonForNoCosts() {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.withreasonfornocosts.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expectedOffenceDecisions = getFileContentAsJsonArray("converter/decision-saved-event.withreasonfornocosts.output.json");
        assertEquals(expectedOffenceDecisions.toString(), actualPayload.toString(), getCustomComparator());
    }

    @Test
    public void shouldConvertReferForCourtHearing() {

        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.referforcourthearing.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expectedOffenceDecisions = getFileContentAsJsonArray("converter/decision-saved-event.referforcourthearing.output.json");

        assertEquals(expectedOffenceDecisions.toString(), actualPayload.toString(), getCustomComparator());
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                // Ignore randomly generated IDs during the validation
                new Customization("[0].id", (o1, o2) -> true),
                new Customization("[1].id", (o1, o2) -> true)
        );
    }

}
