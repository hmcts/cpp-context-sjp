package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.query.view.matcher.IsEqualJSON.equalToJSON;
import static uk.gov.moj.cpp.sjp.query.view.matcher.ResultMatchers.FO;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJsonArray;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

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
    private static final String ENDORSEMENT_NO_POINTS_CODE = "LEN";
    private static final String ENDORSEMENT_WITH_PENALTY_POINTS_CODE = "LEP";
    private static final String ENDORSEMENT_WITH_ADDITIONAL_POINTS_CODE = "LEA";
    private static final String DISQUALIFICATION_ORDINARY_CODE = "DDD";
    private static final String POINTS_DISQUALIFICATION_CODE = "DDP";
    private static final String OBLIGATORY_DISQUALIFICATION_CODE = "DDO";
    private static final String NO_SEPARATE_PENALTY_CODE = "NSP";

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
    private static final UUID ENDORSEMENT_NO_POINTS_RESULT_TYPE_ID = UUID.fromString("0bd4ce39-2055-4fae-9087-dfd7e5ddd449");
    private static final UUID ENDORSEMENT_WITH_PENALTY_POINTS_RESULT_TYPE_ID = UUID.fromString("2391bb68-579f-4c3b-bc12-9d6ff681668d");
    private static final UUID ENDORSEMENT_WITH_ADDITIONAL_POINTS_RESULT_TYPE_ID = UUID.fromString("ad511d29-af34-4db1-841e-77b174f16bc7");
    private static final UUID DISQUALIFICATION_ORDINARY_RESULT_TYPE_ID = UUID.fromString("b8c354b4-be3d-4f73-b313-65e0b92f0d86");
    private static final UUID POINTS_DISQUALIFICATION_RESULT_TYPE_ID = UUID.fromString("dd016c2e-7745-4c4c-a02d-cb899bc10aee");
    private static final UUID OBLIGATORY_DISQUALIFICATION_RESULT_TYPE_ID = UUID.fromString("b96f6aae-ed91-4e39-a85d-cfd440288a0e");
    private static final UUID NO_SEPARATE_PENALTY_RESULT_TYPE_ID = UUID.fromString("49939c7c-750f-403e-9ce1-f82e3e568065");
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    private final UUID OFFENCE1_ID = randomUUID();
    private final UUID OFFENCE2_ID = randomUUID();
    private final UUID OFFENCE3_ID = randomUUID();
    private final UUID OFFENCE4_ID = randomUUID();
    private final UUID OFFENCE5_ID = randomUUID();
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
                        .add("id", ENDORSEMENT_NO_POINTS_RESULT_TYPE_ID.toString())
                        .add("code", ENDORSEMENT_NO_POINTS_CODE).build(),
                createObjectBuilder()
                        .add("id", ENDORSEMENT_WITH_PENALTY_POINTS_RESULT_TYPE_ID.toString())
                        .add("code", ENDORSEMENT_WITH_PENALTY_POINTS_CODE).build(),
                createObjectBuilder()
                        .add("id", ENDORSEMENT_WITH_ADDITIONAL_POINTS_RESULT_TYPE_ID.toString())
                        .add("code", ENDORSEMENT_WITH_ADDITIONAL_POINTS_CODE).build(),
                createObjectBuilder()
                        .add("id", DISQUALIFICATION_ORDINARY_RESULT_TYPE_ID.toString())
                        .add("code", DISQUALIFICATION_ORDINARY_CODE).build(),
                createObjectBuilder()
                        .add("id", POINTS_DISQUALIFICATION_RESULT_TYPE_ID.toString())
                        .add("code", POINTS_DISQUALIFICATION_CODE).build(),
                createObjectBuilder()
                        .add("id", OBLIGATORY_DISQUALIFICATION_RESULT_TYPE_ID.toString())
                        .add("code", OBLIGATORY_DISQUALIFICATION_CODE).build(),
                createObjectBuilder()
                        .add("id", NO_SEPARATE_PENALTY_RESULT_TYPE_ID.toString())
                        .add("code", NO_SEPARATE_PENALTY_CODE).build(),
                createObjectBuilder()
                        .add("id", REFERRED_RESULT_TYPE_ID.toString())
                        .add("code", REFERRED_RESULT_CODE).build(),
                createObjectBuilder()
                        .add("id", ResultCode.NCR.getResultDefinitionId().toString())
                        .add("code", ResultCode.NCR.toString()).build(),
                createObjectBuilder()
                        .add("id", ResultCode.D45.getResultDefinitionId().toString())
                        .add("code", ResultCode.D45.toString()).build(),
                createObjectBuilder()
                        .add("id", ResultCode.DPR.getResultDefinitionId().toString())
                        .add("code", ResultCode.DPR.toString()).build(),
                createObjectBuilder()
                        .add("id", ResultCode.WDRNNOT.getResultDefinitionId().toString())
                        .add("code", ResultCode.WDRNNOT.toString()).build()
        );

        final JsonObject withdrawReasonId = createObjectBuilder()
                .add("id", "1dbf0960-51e3-4d90-803d-d54cd8ea7d3e")
                .add("reasonCodeDescription", "Reason")
                .build();

        when(referenceDataService.getWithdrawalReasons(any())).thenReturn(asList(withdrawReasonId));
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

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.fine.output.json");
        assertThat(expected.toString(), equalToJSON(actualPayload.toString(), getCustomComparator()));
    }

    @Test
    public void shouldNotIncludeFOResultWhenFineIsZeroForFinancialPenalty() {
        // Given
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.fine.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .put("fine", 0)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        // When
        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        // Then
        final JsonObject result = getOffenceDecisionByOffenceId(actualPayload, OFFENCE2_ID);
        assertThat(result.getJsonArray("results"), not(hasItem(FO(BigDecimal.ZERO))));
    }

    @Test
    public void shouldNotIncludeFOResultWhenFineIsZeroWithDecimalsForFinancialPenalty() {
        // Given
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.fine.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .put("fine", new BigDecimal("0.00"))
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        // When
        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        // Then
        final JsonObject result = getOffenceDecisionByOffenceId(actualPayload, OFFENCE2_ID);
        assertThat(result.getJsonArray("results"), not(hasItem(FO(BigDecimal.ZERO))));
    }

    @Test
    public void shouldIncludeFOResultWhenFineIsPresentWithDecimalsForFinancialPenalty() {
        // Given
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.fine.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .put("fine", new BigDecimal("0.01"))
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        // When
        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        // Then
        final JsonObject result = getOffenceDecisionByOffenceId(actualPayload, OFFENCE2_ID);
        assertThat(result.getJsonArray("results"), hasItem(FO(new BigDecimal("0.01"))));
    }

    @Test
    public void shouldConvertFinancialPenaltyWithEndorsement() {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.fine.endorsement.input.json",
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

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.fine.endorsement.output.json");
        assertThat(expected.toString(), equalToJSON(actualPayload.toString(), getCustomComparator()));
    }

    @Test
    public void shouldConvertFinancialPenaltyWithDisqualification() {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.fine.disqualification.input.json",
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

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.fine.disqualification.output.json");
        assertThat(expected.toString(), equalToJSON(actualPayload.toString(), getCustomComparator()));
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

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.discharge.output.json");
        assertThat(expected.toString(), equalToJSON(actualPayload.toString(), getCustomComparator()));
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

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.novictimsurcharge.output.json");
        assertThat(expected.toString(), equalToJSON(actualPayload.toString(), getCustomComparator()));
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

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.withreasonfornocosts.output.json");
        assertThat(expected.toString(), equalToJSON(actualPayload.toString(), getCustomComparator()));
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

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.referforcourthearing.output.json");
        assertThat(expected.toString(), equalToJSON(actualPayload.toString(), getCustomComparator()));
    }

    @Test
    public void shouldConvertNoSeparatePenalty() {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.no-separate-penalty.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray result = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.no-separate-penalty.output.json");
        assertThat(expected.toString(), equalToJSON(result.toString(), getCustomComparator()));
    }

    @Test
    public void shouldConvertNoSeparatePenaltyWithLicenceEndorsementTrue() {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.no-separate-penalty-LEN.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray result = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.no-separate-penalty-LEN.output.json");
        assertThat(expected.toString(), equalToJSON(result.toString(), getCustomComparator()));
    }

    @Test
    public void shouldConvertPressRestrictionForAllOffences() {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.press-restriction.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .put("offence3Id", OFFENCE3_ID)
                                .put("offence4Id", OFFENCE4_ID)
                                .put("offence5Id", OFFENCE5_ID)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray results = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.press-restriction.output.json");
        assertThat(expected.toString(), equalToJSON(results.toString(), getCustomComparator()));
    }

    @Test
    public void shouldConvertPressRestrictionRevokedForAllOffences() {
        final String resultedOn = DATE_FORMAT.format(ZonedDateTime.now());
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.press-restriction-revoked.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", resultedOn)
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .put("offence3Id", OFFENCE3_ID)
                                .put("offence4Id", OFFENCE4_ID)
                                .put("offence5Id", OFFENCE5_ID)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray results = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.press-restriction-revoked.output.json",
                ImmutableMap.<String, Object>builder()
                        .put("revokedOn", resultedOn)
                        .build());
        assertThat(expected.toString(), equalToJSON(results.toString(), getCustomComparator()));
    }

    @Test
    public void shouldConvertPressRestrictionDecisionForAdjournOffences() {
        final String resultedOn = DATE_FORMAT.format(ZonedDateTime.now());
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.press-restriction-revoked.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", resultedOn)
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .put("offence3Id", OFFENCE3_ID)
                                .put("offence4Id", OFFENCE4_ID)
                                .put("offence5Id", OFFENCE5_ID)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray results = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.press-restriction-revoked.output.json",
                ImmutableMap.<String, Object>builder()
                        .put("revokedOn", resultedOn)
                        .build());
        assertThat(expected.toString(), equalToJSON(results.toString(), getCustomComparator()));
    }

    @Test
    public void shouldConvertPressRestrictionDecisionForReferForCourtHearingOffences() {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.press-restriction-referforcourthearing.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));
        when(referenceDataService.getResultIds(decisionSavedEvent)).thenReturn(resultIds);

        final JsonArray results = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.press-restriction-referforcourthearing.output.json",
                ImmutableMap.<String, Object>builder()
                        .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                        .build());
        assertThat(expected.toString(), equalToJSON(results.toString(), getCustomComparator()));
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                // Ignore randomly generated IDs during the validation
                new Customization("[0].id", (o1, o2) -> true),
                new Customization("[1].id", (o1, o2) -> true),
                new Customization("[2].id", (o1, o2) -> true),
                new Customization("[3].id", (o1, o2) -> true),
                new Customization("[4].id", (o1, o2) -> true)
        );
    }

    private JsonObject getOffenceDecisionByOffenceId(final JsonArray json, final UUID offenceId) {
        final Optional<JsonObject> offenceDecision = json.getValuesAs(JsonObject.class)
                .stream()
                .filter(obj -> obj.getString("id") != null)
                .filter(obj -> obj.getString("id").equals(offenceId.toString()))
                .findFirst();
        return offenceDecision.orElseThrow(() -> new RuntimeException("offenceDecision not present for id: " + offenceId));
    }
}
