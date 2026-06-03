package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.api.util.FileUtil.getFileContentAsJson;

import uk.gov.moj.cpp.sjp.query.service.OffenceFineLevels;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


public class OffenceHelperTest {

    private OffenceHelper offenceHelper = new OffenceHelper();


    final static String offenceId_1 = UUID.randomUUID().toString();
    final static String offenceId_2 = UUID.randomUUID().toString();
    final static String offenceId_3 = UUID.randomUUID().toString();

    final static String WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE = "030d4335-f9fe-39e0-ad7e-d01a0791ff87";
    final static String WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED = "a11670b7-681a-39dc-951e-f2f1c24fb4c9";

    @Test
    public void shouldGetMaxFineLevel() {
        final String offence1Code = "CA03010";
        final String offence2Code = "RT88461";
        final JsonObject offenceDefinition1 =  getFileContentAsJson(format("offence-reference-data/%s.json",offence1Code));
        assertThat("3", Matchers.equalTo(offenceHelper.getMaxFineLevel(offenceDefinition1)));

        final JsonObject offenceDefinition2 =  getFileContentAsJson(format("offence-reference-data/%s.json",offence2Code));
        assertThat("", Matchers.equalTo(offenceHelper.getMaxFineLevel(offenceDefinition2)));

    }

    @Test
    public void shouldReturnEmptyMaxFineValueForUnlimitedFine() {
        final String offence1Code = "MT03001";
        final String offence2Code = "PS00001";
        final OffenceFineLevels offenceFineLevels = null;

        final JsonObject offenceDefinition1 =  getFileContentAsJson(format("offence-reference-data/%s.json",offence1Code));
        Optional<BigDecimal> maxFineValue1 = offenceHelper.getMaxFineValue(offenceDefinition1, offenceFineLevels);
        assertFalse(maxFineValue1.isPresent());

        final JsonObject offenceDefinition2 =  getFileContentAsJson(format("offence-reference-data/%s.json",offence2Code));
        Optional<BigDecimal> maxFineValue2 = offenceHelper.getMaxFineValue(offenceDefinition2, offenceFineLevels);
        assertFalse(maxFineValue2.isPresent());

    }

    @Test
    public void shouldCalculateIsBackDuty() {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        assertThat(offenceHelper.isBackDuty(offenceDefinition.build()), equalTo(false));
        offenceDefinition.add("backDuty", true);
        assertThat(offenceHelper.isBackDuty(offenceDefinition.build()), equalTo(true));
    }

    @Test
    public void shouldCalculatePenaltyType() {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        assertThat(offenceHelper.getPenaltyType(offenceDefinition.build()), equalTo(StringUtils.EMPTY));
        offenceDefinition.add("penaltyType", "Excise penalty");
        assertThat(offenceHelper.getPenaltyType(offenceDefinition.build()), equalTo("Excise penalty"));
    }

    @Test
    public void shouldCalculateSentencing() {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        assertThat(offenceHelper.getSentencing(offenceDefinition.build()), equalTo(StringUtils.EMPTY));
        offenceDefinition.add("sentencing", "Yes");
        assertThat(offenceHelper.getSentencing(offenceDefinition.build()), equalTo("Yes"));
    }

    @Test
    public void hasFinalDecision() {

        final String offence1Code = "CA03010";
        final String offence2Code = "PS00001";
        final String offence3Code = "RT88461";

        final LocalDate offence1CommittedDate = now().minusDays(1);
        final LocalDate offence2CommittedDate = now().minusDays(2);
        final LocalDate offence3CommittedDate = now().minusDays(3);

        final JsonObject offence1 = createObjectBuilder()
                .add("id", offenceId_1)
                .add("offenceCode", offence1Code)
                .add("startDate", offence1CommittedDate.toString())
                .add("completed", true)
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)
                .build();

        final JsonObject offence2 = createObjectBuilder()
                .add("id", offenceId_2)
                .add("offenceCode", offence2Code)
                .add("completed", true)
                .add("startDate", offence2CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)
                .build();

        final JsonObject offence3 = createObjectBuilder()
                .add("id", offenceId_3)
                .add("offenceCode", offence3Code)
                .add("completed", false)
                .add("startDate", offence3CommittedDate.toString())
                .build();

        assertTrue(offenceHelper.hasFinalDecision(offence1, getCaseDecisions()));
        assertTrue(offenceHelper.hasFinalDecision(offence2, getCaseDecisions()));

        assertFalse(offenceHelper.hasFinalDecision(offence3, getCaseDecisions()));

        assertFalse(offenceHelper.hasFinalDecision(offence1, getCaseDecisionsWithSetAside()));
        assertFalse(offenceHelper.hasFinalDecision(offence2, getCaseDecisionsWithSetAside()));
        assertFalse(offenceHelper.hasFinalDecision(offence3, getCaseDecisionsWithSetAside()));
    }

    @Test
    public void caseHasNoDecisionHasFinalDecisionReturnsFalse(){
        final String offence1Code = "CA03010";
        final String offence2Code = "PS00001";
        final String offence3Code = "RT88461";

        final LocalDate offence1CommittedDate = now().minusDays(1);
        final LocalDate offence2CommittedDate = now().minusDays(2);
        final LocalDate offence3CommittedDate = now().minusDays(3);

        final JsonObject offence1 = createObjectBuilder()
                .add("id", offenceId_1)
                .add("offenceCode", offence1Code)
                .add("startDate", offence1CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)
                .build();

        final JsonObject offence2 = createObjectBuilder()
                .add("id", offenceId_2)
                .add("offenceCode", offence2Code)
                .add("startDate", offence2CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)
                .build();

        final JsonObject offence3 = createObjectBuilder()
                .add("id", offenceId_3)
                .add("offenceCode", offence3Code)
                .add("startDate", offence3CommittedDate.toString())
                .build();

        assertFalse(offenceHelper.hasFinalDecision(offence1, createArrayBuilder().build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Either Way", "Indictable"})
    void shouldBeNonSummaryOffenceAsTrueWhenModeOfTrialDerivedINonSummary(final String modeOfTrialsDerived) {
        final JsonObject offenceDefinition = createObjectBuilder()
                .add("modeOfTrialDerived", modeOfTrialsDerived)
                .build();

        assertTrue(offenceHelper.isNonSummaryOffence(offenceDefinition));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Summary"})
    @NullAndEmptySource
    void shouldBeNonSummaryOffenceAsFalseWhenModeOfTrialDerivedIsSummary(final String modeOfTrialsDerived) {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        if(nonNull(modeOfTrialsDerived)){
            offenceDefinition.add("modeOfTrialDerived", modeOfTrialsDerived);
        }

        assertFalse(offenceHelper.isNonSummaryOffence(offenceDefinition.build()));
    }



    private static JsonArray getCaseDecisions() {
        final ZonedDateTime savedAt = ZonedDateTime.parse("2018-12-28T11:53:04.693Z");

        JsonArray offenceDecisions = createArrayBuilder().add(createObjectBuilder()
                .add("savedAt", savedAt.toString())
                .add("offenceDecisions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("offenceId", offenceId_1)
                                .add("decisionType", "WITHDRAW")
                                .add("withdrawalReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)

                                .add("verdict", "NO_VERDICT"))
                        .add(createObjectBuilder().add("offenceId", offenceId_2)
                                .add("decisionType", "WITHDRAW")
                                .add("withdrawalReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)

                                .add("verdict", "NO_VERDICT"))
                        .add(createObjectBuilder().add("offenceId", offenceId_3)
                                .add("decisionType", "ADJOURN")
                                .add("verdict", "NO_VERDICT")))).build();

        return offenceDecisions;
    }


    private static JsonArray getCaseDecisionsWithSetAside() {

        final ZonedDateTime savedAt1 = ZonedDateTime.parse("2018-12-28T11:53:04.693Z");
        final ZonedDateTime savedAt2 = ZonedDateTime.parse("2018-12-28T11:53:04.694Z");

        final JsonArray offenceDecisions = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("savedAt", savedAt1.toString())
                        .add("offenceDecisions", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("offenceId", offenceId_1)
                                        .add("decisionType", "WITHDRAW")
                                        .add("withdrawalReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)

                                        .add("verdict", "NO_VERDICT"))
                                .add(createObjectBuilder().add("offenceId", offenceId_2)
                                        .add("decisionType", "WITHDRAW")
                                        .add("withdrawalReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)

                                        .add("verdict", "NO_VERDICT"))
                                .add(createObjectBuilder().add("offenceId", offenceId_3)
                                        .add("decisionType", "ADJOURN")
                                        .add("verdict", "NO_VERDICT")))
                        .build())
                .add(createObjectBuilder()
                        .add("savedAt", savedAt2.toString())
                        .add("offenceDecisions", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("offenceId", offenceId_1)
                                        .add("decisionType", "SET_ASIDE"))
                                .add(createObjectBuilder().add("offenceId", offenceId_2)
                                        .add("decisionType", "SET_ASIDE"))
                                .add(createObjectBuilder().add("offenceId", offenceId_3)
                                        .add("decisionType", "SET_ASIDE"))).build())
                .build();

        return offenceDecisions;
    }
}
