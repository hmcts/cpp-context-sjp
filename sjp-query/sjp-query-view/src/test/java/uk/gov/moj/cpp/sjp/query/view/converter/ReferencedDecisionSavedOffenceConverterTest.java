package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.query.view.matcher.ResultMatchers.FO;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJsonArray;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.util.fakes.FakeReferenceDataService;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferencedDecisionSavedOffenceConverterTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    private final UUID OFFENCE1_ID = randomUUID();
    private final UUID OFFENCE2_ID = randomUUID();
    private final UUID OFFENCE3_ID = randomUUID();
    private final UUID OFFENCE4_ID = randomUUID();
    private final UUID OFFENCE5_ID = randomUUID();
    private final UUID OFFENCE6_ID = randomUUID();
    private final UUID DECISION1_ID = randomUUID();

    @InjectMocks
    private ReferencedDecisionSavedOffenceConverter referencedDecisionSavedOffenceConverter;
    @Spy
    private FakeReferenceDataService referenceDataService = new FakeReferenceDataService();

    @BeforeEach
    public void setup() {
        referenceDataService.addWithdrawalReason(fromString("1dbf0960-51e3-4d90-803d-d54cd8ea7d3e"), "Reason");
    }

    @Test
    public void shouldConvertFinancialPenalty() throws JSONException {
        String path = "converter/decision-saved-event.fine.input.json";
        ImmutableMap<String, Object> decisionObject = ImmutableMap.<String, Object>builder()
                .put("caseId", CASE_ID)
                .put("sessionId", SESSION_ID)
                .put("decisionId", DECISION1_ID)
                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                .put("offence1Id", OFFENCE1_ID)
                .put("offence2Id", OFFENCE2_ID)
                .build();
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson(path,
                        decisionObject));

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        String expectedFilePath = "converter/decision-saved-event.fine.output.json";
        final JsonArray expected = getFileContentAsJsonArray(expectedFilePath);
        assertJsonArraysEqualIgnoringIDs(expected, actualPayload);
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

        // When
        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        // Then
        final JsonObject result = getOffenceDecisionByOffenceId(actualPayload, OFFENCE2_ID);
        assertThat(result.getJsonArray("results"), hasItem(FO(new BigDecimal("0.01"))));
    }

    @Test
    public void shouldConvertFinancialPenaltyWithEndorsement() throws JSONException {
        String path = "converter/decision-saved-event.fine.endorsement.input.json";
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson(path,
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.fine.endorsement.output.json");
        assertJsonArraysEqualIgnoringIDs(expected, actualPayload);
    }

    @Test
    public void shouldConvertFinancialPenaltyWithDisqualification() throws JSONException {
        String path = "converter/decision-saved-event.fine.disqualification.input.json";
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson(path,
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.fine.disqualification.output.json");
        assertJsonArraysEqualIgnoringIDs(expected, actualPayload);
    }

    @Test
    public void shouldConvertAbsoluteDischargeDVLA() throws JSONException {
        String path = "converter/decision-saved-event.discharge.input.json";
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson(path,
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        String expectedFilePath = "converter/decision-saved-event.discharge.output.json";
        final JsonArray expected = getFileContentAsJsonArray(expectedFilePath);
        assertJsonArraysEqualIgnoringIDs(expected, actualPayload);
    }

    @Test
    public void shouldConvertFinancialPenaltyForDVLAWithNoVictimSurcharge() throws JSONException {
        String path = "converter/decision-saved-event.novictimsurcharge.input.json";
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson(path,
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .put("offence2Id", OFFENCE2_ID)
                                .build()));

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        String expectedFilePath = "converter/decision-saved-event.novictimsurcharge.output.json";
        final JsonArray expected = getFileContentAsJsonArray(expectedFilePath);
        assertJsonArraysEqualIgnoringIDs(expected, actualPayload);
    }

    @Test
    public void shouldConvertFinancialPenaltyWithZeroCostsAndReasonForNoCosts() throws JSONException {
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

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.withreasonfornocosts.output.json");
        assertJsonArraysEqualIgnoringIDs(expected, actualPayload);
    }

    @Test
    public void shouldConvertReferForCourtHearing() throws JSONException {
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

        final JsonArray actualPayload = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.referforcourthearing.output.json");
        assertJsonArraysEqualIgnoringIDs(expected, actualPayload);
    }

    @Test
    public void shouldConvertNoSeparatePenalty() throws JSONException {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.no-separate-penalty.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .build()));

        final JsonArray result = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.no-separate-penalty.output.json");
        assertJsonArraysEqualIgnoringIDs(expected, result);
    }

    @Test
    public void shouldConvertNoSeparatePenaltyWithLicenceEndorsementTrue() throws JSONException {
        final JsonEnvelope decisionSavedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-completed"),
                getFileContentAsJson("converter/decision-saved-event.no-separate-penalty-LEN.input.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION1_ID)
                                .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                                .put("offence1Id", OFFENCE1_ID)
                                .build()));

        final JsonArray result = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.no-separate-penalty-LEN.output.json");
        assertJsonArraysEqualIgnoringIDs(expected, result);
    }

    @Test
    public void shouldConvertPressRestrictionForAllOffences() throws JSONException {
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
                                .put("offence6Id", OFFENCE6_ID)
                                .build()));

        final JsonArray results = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.press-restriction.output.json");
        assertJsonArraysEqualIgnoringIDs(expected, results);
    }

    @Test
    public void shouldConvertPressRestrictionRevokedForAllOffences() throws JSONException {
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
                                .put("offence6Id", OFFENCE6_ID)
                                .build()));

        final JsonArray results = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.press-restriction-revoked.output.json",
                ImmutableMap.<String, Object>builder()
                        .put("revokedOn", resultedOn)
                        .build());
        assertJsonArraysEqualIgnoringIDs(expected, results);
    }

    @Test
    public void shouldConvertPressRestrictionDecisionForReferForCourtHearingOffences() throws JSONException {
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

        final JsonArray results = referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent);

        final JsonArray expected = getFileContentAsJsonArray("converter/decision-saved-event.press-restriction-referforcourthearing.output.json",
                ImmutableMap.<String, Object>builder()
                        .put("resultedOn", DATE_FORMAT.format(ZonedDateTime.now()))
                        .build());
        assertJsonArraysEqualIgnoringIDs(expected, results);
    }

    private void assertJsonArraysEqualIgnoringIDs(JsonArray expectedArray, JsonArray actualArray) throws JSONException {
        JsonArray expectedArrayModified = removeIdFromJsonObjects(expectedArray);
        JsonArray actualArrayModified = removeIdFromJsonObjects(actualArray);

        assertEquals(toMap(expectedArrayModified), toMap(actualArrayModified));

    }

    private Map<String, String> toMap(JsonObject jsonObject) {
        return jsonObject.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString().replaceAll("\"", "")));
    }

    private List<Map<String, String>> toMap(JsonArray jsonArray) {
        return jsonArray.getValuesAs(JsonObject.class).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private JsonArray removeIdFromJsonObjects(JsonArray jsonArray) {
        JsonArrayBuilder arrayBuilder = createArrayBuilder();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject originalObject = jsonArray.getJsonObject(i);
            JsonObjectBuilder objectBuilder = createObjectBuilder();

            for (String key : originalObject.keySet()) {
                if (!"id".equals(key)) {
                    objectBuilder.add(key, originalObject.get(key));
                }
            }

            arrayBuilder.add(objectBuilder.build());
        }

        return arrayBuilder.build();
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