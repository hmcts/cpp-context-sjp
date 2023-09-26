package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit;
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
public class DischargeDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private DischargeDecisionResultAggregator aggregator;

    @Mock
    private CourtCentreConverter courtCentreConverter;

    private CourtCentre courtCentre = courtCentre().build();

    @Before
    public void setUp() {
        super.setUp();
        aggregator = new DischargeDecisionResultAggregator(jCachedReferenceData);
        setField(aggregator, "courtCentreConverter", courtCentreConverter);

        when(courtCentreConverter.convertByOffenceId(anyObject(), anyObject())).thenReturn(Optional.of(courtCentre));
    }

    @Test
    public void shouldPopulateAbsoluteDischargeResultWithRightPrompts() {

        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25), false, null, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(6));

        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("b9c6047b-fb84-4b12-97a1-2175e4b8bbac"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Absolute discharge")),
                        hasProperty("judicialResultPrompts", is(nullValue())))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5c5a693f-c26c-4352-bbb3-ac72dd141e88"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("5: DVLA held not produced"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }

    @Test
    public void shouldPopulateConditionalDischargeResultWithRightPrompts() {

        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), CONDITIONAL, new DischargePeriod(2, PeriodUnit.MONTH), BigDecimal.TEN,
                null, false, new BigDecimal(25), false, null, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(6));

        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("554c2622-c1cc-459e-a98d-b7f317ab065c"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("durationElement", Matchers.is(notNullValue())),
                        hasProperty("resultText", Matchers.is("Conditional discharge\n" +
                                "Period of conditional discharge 2 Months")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("d3205319-84cf-4c5b-9d7a-7e4bb1865054"))),
                                hasProperty("value", Matchers.is("2 Months")))
                        )))))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }

    @Test
    public void shouldPopulateBackDutyResultWithRightPrompts() {
        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25), false, null, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(6));

        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5edd3a3a-8dc7-43e4-96c4-10fed16278ac"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Vehicle Excise Back Duty\n" +
                                "Amount of back duty £25.00")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("f7c7c088-f88e-4c28-917c-78571517aca1"))),
                                hasProperty("value", Matchers.is("£25.00")))
                        ))))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5c5a693f-c26c-4352-bbb3-ac72dd141e88"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("5: DVLA held not produced"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)),is(true));
    }

    @Test
    public void shouldPopulateResultLicenseEndorsementPenaltyPointsImposedWithNoReason() {
        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25), true, 21, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(7));
        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("cee54856-4450-4f28-a8a9-72b688726201"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Driving record endorsed with penalty points\n" +
                                "Penalty points for this offence 21\n" +
                                "Defendant driving licence number drivernumber")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("a8719de4-7783-448a-b792-e3f94e670ad0"))),
                                        hasProperty("value", Matchers.is("21")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("a593ae4a-9d69-45b9-9585-c11aeed28404"))),
                                        hasProperty("value", Matchers.is("drivernumber")))
                                ))))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5c5a693f-c26c-4352-bbb3-ac72dd141e88"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("5: DVLA held not produced"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }

    @Test
    public void shouldPopulateResultLicenseEndorsementPenaltyPointsImposedWithReason() {
        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25), true, 21, PenaltyPointsReason.DIFFERENT_OCCASIONS, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(7));
        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("3fa139cc-efe0-422b-93d6-190a5be50953"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Driving record endorsed with additional points (points on more than one offence)\n" +
                                "Penalty points for this offence 21\n" +
                                "Reasons for imposing penalty points on more than one offence DIFFERENT_OCCASIONS\n" +
                                "Defendant driving licence number drivernumber")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("a8719de4-7783-448a-b792-e3f94e670ad0"))),
                                        hasProperty("value", Matchers.is("21")),
                                        hasProperty("totalPenaltyPoints", Matchers.is(new BigDecimal(21))))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("a593ae4a-9d69-45b9-9585-c11aeed28404"))),
                                        hasProperty("value", Matchers.is("drivernumber")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("bbbb47bb-3418-463c-bfc3-43c6f72bb7c9"))),
                                        hasProperty("value", Matchers.is("DIFFERENT_OCCASIONS")))
                                ))))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5c5a693f-c26c-4352-bbb3-ac72dd141e88"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("5: DVLA held not produced"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }


    @Test
    public void shouldPopulateResultLicenseEndorsementWithNoPenaltyPointsImposed() {
        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25), true, null, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(7));
        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("b0aeb4fc-df63-4e2f-af88-97e3f23e847f"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Driving record endorsed (no points)")),
                        hasProperty("judicialResultPrompts", is(nullValue())))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5c5a693f-c26c-4352-bbb3-ac72dd141e88"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("5: DVLA held not produced"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }

    @Test
    public void shouldPopulateCompensationResult() {
        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25), true, null, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(7));
        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ae89b99c-e0e3-47b5-b218-24d4fca3ca53"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Compensation\nAmount of compensation £10.00\nMajor creditor Television Licensing Organisation")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("26985e5b-fe1f-4d7d-a21a-57207c5966e7"))),
                                hasProperty("value", Matchers.is("£10.00")))
                        ))))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5c5a693f-c26c-4352-bbb3-ac72dd141e88"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("5: DVLA held not produced"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }

    @Test
    public void shouldPopulateNoCompensationReasonResult() {
        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                "no compensation reason", false, new BigDecimal(25), true, null, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, "drivernumber", "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(8));
        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("29e02fa1-42ce-4eec-914e-e62508397a16"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("No compensation reason\nReason for no compensation no compensation reason")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("e263de82-47ca-433a-bb41-cad2e1c5bb72"))),
                                hasProperty("value", Matchers.is("no compensation reason")))
                        ))))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("5c5a693f-c26c-4352-bbb3-ac72dd141e88"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("5: DVLA held not produced"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }

    @Test
    public void shouldPopulateNoCompensationReasonResultWithoutDriverNumber() {
        final Discharge offenceDecision = new Discharge(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                "no compensation reason", false, new BigDecimal(25), true, null, null, null,
                true, DisqualificationType.DISCRETIONARY, new DisqualificationPeriod(2, DisqualificationPeriodTimeUnit.MONTH), null, null);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn, null, "TVL");

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(8));
        assertThat(resultsAggregate.getResults(offence1Id), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("29e02fa1-42ce-4eec-914e-e62508397a16"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("No compensation reason\nReason for no compensation no compensation reason")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("e263de82-47ca-433a-bb41-cad2e1c5bb72"))),
                                hasProperty("value", Matchers.is("no compensation reason")))
                        ))))),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("ea1ee5a4-be13-48dc-8411-78f22e01c236"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Licence produced in court"))
                )),
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("c3a39c2f-b056-442f-8dfc-604b5434f956"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("0: None or unknown"))
                ))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }

}
