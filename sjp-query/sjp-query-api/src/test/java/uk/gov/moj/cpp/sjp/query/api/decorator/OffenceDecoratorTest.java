package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.query.api.util.FileUtil.getFileContentAsJson;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.service.OffenceFineLevels;
import uk.gov.moj.cpp.sjp.query.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceDecoratorTest {

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private OffenceHelper offenceHelper;

    @Mock
    private WithdrawalReasons withdrawalReasons;

    @Mock
    private OffenceFineLevels offenceFineLevels;

    @InjectMocks
    private OffenceDecorator offenceDecorator;

    public static final String offenceId_1 = UUID.randomUUID().toString();
    public static final String offenceId_2 = UUID.randomUUID().toString();
    public static final String offenceId_3 = UUID.randomUUID().toString();

    public static final JsonArray caseDecisions = getCaseDecisions();


    private static final String WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE = "030d4335-f9fe-39e0-ad7e-d01a0791ff87";
    private static final String WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED = "a11670b7-681a-39dc-951e-f2f1c24fb4c9";

    @Test
    public void shouldDecorateAllOffences() {

        final String offence1Code = "CA03010";
        final String offence2Code = "PS00001";
        final String offence3Code = "RT88461";

        final LocalDate offence1CommittedDate = now().minusDays(1);
        final LocalDate offence2CommittedDate = now().minusDays(2);
        final LocalDate offence3CommittedDate = now().minusDays(3);


        final String INSUFFICIENT_EVIDENCE = "Insufficient Evidence";
        final String NOT_IN_PUBLIC_INTEREST_TO_PROCEED = "Not in public interest to proceed";

        final JsonObject offence1 = createObjectBuilder()
                .add("offenceCode", offence1Code)
                .add("startDate", offence1CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)
                .build();

        final JsonObject offence2 = createObjectBuilder()
                .add("offenceCode", offence2Code)
                .add("startDate", offence2CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)
                .build();

        final JsonObject offence3 = createObjectBuilder()
                .add("offenceCode", offence3Code)
                .add("startDate", offence3CommittedDate.toString())
                .build();

        final JsonObject originalCase = buildCaseWithOffences(offence1, offence2, offence3);

        final JsonObject offenceDefinition1 =  getFileContentAsJson(format("offence-reference-data/%s.json", offence1Code));
        final JsonObject offenceDefinition2 =  getFileContentAsJson(format("offence-reference-data/%s.json", offence2Code));
        final JsonObject offenceDefinition3 =  getFileContentAsJson(format("offence-reference-data/%s.json", offence3Code));

        when(referenceDataService.getOffenceDefinition(offence1Code, offence1CommittedDate.toString(), jsonEnvelope)).thenReturn(offenceDefinition1);
        when(referenceDataService.getOffenceDefinition(offence2Code, offence2CommittedDate.toString(), jsonEnvelope)).thenReturn(offenceDefinition2);
        when(referenceDataService.getOffenceDefinition(offence3Code, offence3CommittedDate.toString(), jsonEnvelope)).thenReturn(offenceDefinition3);

        when(offenceHelper.getEnglishTitle(offenceDefinition1)).thenReturn("title1");
        when(offenceHelper.getEnglishTitle(offenceDefinition2)).thenReturn("title2");
        when(offenceHelper.getEnglishTitle(offenceDefinition3)).thenReturn("title3");

        when(offenceHelper.getWelshTitle(offenceDefinition1)).thenReturn(of("welshTitle1"));
        when(offenceHelper.getWelshTitle(offenceDefinition2)).thenReturn(empty());
        when(offenceHelper.getWelshTitle(offenceDefinition3)).thenReturn(of("welshTitle3"));

        when(offenceHelper.getEnglishLegislation(offenceDefinition1)).thenReturn("legislation1");
        when(offenceHelper.getEnglishLegislation(offenceDefinition2)).thenReturn("legislation2");
        when(offenceHelper.getEnglishLegislation(offenceDefinition3)).thenReturn("legislation3");

        when(offenceHelper.getWelshLegislation(offenceDefinition1)).thenReturn(empty());
        when(offenceHelper.getWelshLegislation(offenceDefinition2)).thenReturn(empty());
        when(offenceHelper.getWelshLegislation(offenceDefinition3)).thenReturn(of("welshLegislation3"));

        when(offenceHelper.isOffenceNotInEffect(offence1, offenceDefinition1)).thenReturn(false);
        when(offenceHelper.isOffenceNotInEffect(offence2, offenceDefinition2)).thenReturn(true);
        when(offenceHelper.isOffenceNotInEffect(offence3, offenceDefinition3)).thenReturn(false);

        when(offenceHelper.isOffenceOutOfTime(offence1, offenceDefinition1)).thenReturn(true);
        when(offenceHelper.isOffenceOutOfTime(offence2, offenceDefinition2)).thenReturn(false);
        when(offenceHelper.isOffenceOutOfTime(offence3, offenceDefinition3)).thenReturn(false);

        when(offenceHelper.isOffenceImprisonable(offenceDefinition1)).thenReturn(false);
        when(offenceHelper.isOffenceImprisonable(offenceDefinition2)).thenReturn(true);
        when(offenceHelper.isOffenceImprisonable(offenceDefinition3)).thenReturn(false);

        when(offenceHelper.getMaxFineLevel(offenceDefinition1)).thenReturn("3");
        when(offenceHelper.getMaxFineLevel(offenceDefinition2)).thenReturn("");
        when(offenceHelper.getMaxFineLevel(offenceDefinition3)).thenReturn("3");

        when(offenceHelper.getWithdrawalRequestReason(offence1, withdrawalReasons)).thenReturn(of(INSUFFICIENT_EVIDENCE));
        when(offenceHelper.getWithdrawalRequestReason(offence2, withdrawalReasons)).thenReturn(of(NOT_IN_PUBLIC_INTEREST_TO_PROCEED));
        when(offenceHelper.getWithdrawalRequestReason(offence3, withdrawalReasons)).thenReturn(empty());

        when(offenceHelper.getMaxFineValue(offenceDefinition1, offenceFineLevels)).thenReturn(of(BigDecimal.valueOf(1000)));
        when(offenceHelper.getMaxFineValue(offenceDefinition2, offenceFineLevels)).thenReturn(empty());
        when(offenceHelper.getMaxFineValue(offenceDefinition3, offenceFineLevels)).thenReturn(of(BigDecimal.valueOf(2500)));

        final JsonObject expectedDecoratedOffence1 = createObjectBuilder()
                .add("offenceCode", offence1Code)
                .add("startDate", offence1CommittedDate.toString())
                .add("title", "title1")
                .add("titleWelsh", "welshTitle1")
                .add("legislation", "legislation1")
                .add("notInEffect", false)
                .add("outOfTime", true)
                .add("imprisonable", false)
                .add("isNonSummaryOffence", true)
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)
                .add("withdrawalRequestReason", INSUFFICIENT_EVIDENCE)
                .add("maxFineLevel", "3")
                .add("hasFinalDecision", true)
                .add("pendingWithdrawal", false)
                .add("backDutyOffence", false)
                .add("penaltyType", EMPTY)
                .add("sentencing", EMPTY)
                .add("maxFineValue", BigDecimal.valueOf(1000))

                .build();

        final JsonObject expectedDecoratedOffence2 = createObjectBuilder()
                .add("offenceCode", offence2Code)
                .add("startDate", offence2CommittedDate.toString())
                .add("title", "title2")
                .add("legislation", "legislation2")
                .add("notInEffect", true)
                .add("outOfTime", false)
                .add("imprisonable", true)
                .add("isNonSummaryOffence", false)
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)
                .add("withdrawalRequestReason", NOT_IN_PUBLIC_INTEREST_TO_PROCEED)
                .add("maxFineLevel", "")
                .add("hasFinalDecision", true)
                .add("pendingWithdrawal", false)
                .add("backDutyOffence", true)
                .add("penaltyType", "Excise penalty")
                .add("sentencing", "Yes")

                .build();

        final JsonObject expectedDecoratedOffence3 = createObjectBuilder()
                .add("offenceCode", offence3Code)
                .add("startDate", offence3CommittedDate.toString())
                .add("title", "title3")
                .add("titleWelsh", "welshTitle3")
                .add("legislation", "legislation3")
                .add("legislationWelsh", "welshLegislation3")
                .add("notInEffect", false)
                .add("outOfTime", false)
                .add("imprisonable", false)
                .add("isNonSummaryOffence", true)
                .add("maxFineLevel", "3")
                .add("hasFinalDecision", false)
                .add("pendingWithdrawal", false)
                .add("backDutyOffence", false)
                .add("penaltyType", "Excise penalty")
                .add("sentencing", "Yes")
                .add("maxFineValue", BigDecimal.valueOf(2500))
                .build();

        when(offenceHelper.hasFinalDecision(offence1, caseDecisions)).thenReturn(true);
        when(offenceHelper.hasFinalDecision(offence2, caseDecisions)).thenReturn(true);
        when(offenceHelper.hasFinalDecision(offence3, caseDecisions)).thenReturn(false);

        when(offenceHelper.isBackDuty(offenceDefinition1)).thenReturn(false);
        when(offenceHelper.isBackDuty(offenceDefinition2)).thenReturn(true);
        when(offenceHelper.isBackDuty(offenceDefinition3)).thenReturn(false);

        when(offenceHelper.getPenaltyType(offenceDefinition1)).thenReturn(EMPTY);
        when(offenceHelper.getPenaltyType(offenceDefinition2)).thenReturn("Excise penalty");
        when(offenceHelper.getPenaltyType(offenceDefinition3)).thenReturn("Excise penalty");

        when(offenceHelper.getSentencing(offenceDefinition1)).thenReturn(EMPTY);
        when(offenceHelper.getSentencing(offenceDefinition2)).thenReturn("Yes");
        when(offenceHelper.getSentencing(offenceDefinition3)).thenReturn("Yes");

        when(offenceHelper.isNonSummaryOffence(offenceDefinition1)).thenReturn(true);
        when(offenceHelper.isNonSummaryOffence(offenceDefinition2)).thenReturn(false);
        when(offenceHelper.isNonSummaryOffence(offenceDefinition3)).thenReturn(true);

        final JsonObject expectedDecoratedCase = buildCaseWithOffences(expectedDecoratedOffence1, expectedDecoratedOffence2, expectedDecoratedOffence3);

        final JsonObject actualDecoratedCase = offenceDecorator.decorateAllOffences(originalCase, jsonEnvelope, withdrawalReasons, offenceFineLevels);

        assertThat(actualDecoratedCase, equalTo(expectedDecoratedCase));
    }

    private static JsonObject buildCaseWithOffences(final JsonObject... offences) {
        final JsonArrayBuilder offencesArray = stream(offences).reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add);
        return createObjectBuilder()
                .add("caseDecisions", caseDecisions)
                .add("defendant", createObjectBuilder()
                        .add("offences", offencesArray)).build();
    }

    private static JsonArray getCaseDecisions() {

        return createArrayBuilder().add(createObjectBuilder()
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
    }
}
