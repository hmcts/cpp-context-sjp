package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriodTimeUnit;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.CourtCentreConverter;

import java.math.BigDecimal;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FinancialPenaltyDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private FinancialPenaltyDecisionResultAggregator aggregator;

    @Mock
    private CourtCentreConverter courtCentreConverter;

    private CourtCentre courtCentre = courtCentre().build();

    @Before
    public void setUp() {
        super.setUp();
        aggregator = new FinancialPenaltyDecisionResultAggregator(jCachedReferenceData);
        setField(aggregator, "courtCentreConverter", courtCentreConverter);
        when(courtCentreConverter.convertByOffenceId(anyObject(), anyObject())).thenReturn(Optional.of(courtCentre));
    }

    @Test
    public void shouldPopulateBasicResultWithRightPrompts() {

        final FinancialPenalty offenceDecision
                = new FinancialPenalty(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP),
                BigDecimal.ONE,
                BigDecimal.TEN,
                "no compensationReason",
                true,
                BigDecimal.ONE,
                BigDecimal.ONE,
                true,
                2,
                PenaltyPointsReason.DIFFERENT_OCCASIONS,
                "additional points reason",
                true,
                DisqualificationType.POINTS,
                new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH),
                1,
                PressRestriction.requested("name"));

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber" , "TVL");

        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("969f150c-cd05-46b0-9dd9-30891efcc766"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Fine\nAmount of fine £1.00")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("7cd1472f-2379-4f5b-9e67-98a43d86e122"))),
                                hasProperty("value", Matchers.is("£1.00")))
                        )))))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)),Matchers.is(true));

    }


    @Test
    public void shouldPopulateExcisePenaltyResult() {

        final FinancialPenalty offenceDecision
                = new FinancialPenalty(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP),
                BigDecimal.ONE,
                BigDecimal.TEN,
                "no compensationReason",
                true,
                BigDecimal.ONE,
                BigDecimal.TEN,
                true,
                2,
                PenaltyPointsReason.DIFFERENT_OCCASIONS,
                "additional points reason",
                true,
                DisqualificationType.POINTS,
                new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH),
                1,
                PressRestriction.requested("name"));

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber" , "TVL");

        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("fcb26a5f-28cc-483e-b430-d823fac808df"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Excise penalty\nAmount of excise penalty £10.00")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("b3dfed9a-efba-4126-a08d-cf37d18b4563"))),
                                hasProperty("value", Matchers.is("£10.00")))
                        ))))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("73fe22ca-76bd-4aba-bdea-6dfef8ee03a2"))),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("2bf54447-328c-4c1b-a123-341adbd52172"))),
                                hasProperty("value", Matchers.is("2 Months")))
                        )))))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)),Matchers.is(true));

    }
}
