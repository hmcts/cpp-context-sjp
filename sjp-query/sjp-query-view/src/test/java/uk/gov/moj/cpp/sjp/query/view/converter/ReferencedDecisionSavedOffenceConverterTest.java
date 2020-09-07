package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.query.view.matcher.IsEqualJSON.equalToJSON;
import static uk.gov.moj.cpp.sjp.query.view.matcher.ResultMatchers.FO;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJsonArray;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.util.fakes.FakeReferenceDataService;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setup() {
        referenceDataService.addWithdrawalReason(fromString("1dbf0960-51e3-4d90-803d-d54cd8ea7d3e"), "Reason");
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
                                .put("offence6Id", OFFENCE6_ID)
                                .build()));

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
                                .put("offence6Id", OFFENCE6_ID)
                                .build()));

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
                new Customization("[4].id", (o1, o2) -> true),
                new Customization("[5].id", (o1, o2) -> true)
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
