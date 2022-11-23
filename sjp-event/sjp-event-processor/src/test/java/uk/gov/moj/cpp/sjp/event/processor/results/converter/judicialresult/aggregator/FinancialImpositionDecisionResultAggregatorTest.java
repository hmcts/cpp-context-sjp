package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.ATTACH_TO_EARNINGS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.DEDUCT_FROM_BENEFITS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class FinancialImpositionDecisionResultAggregatorTest extends BaseDecisionResultAggregatorTest {

    private FinancialImpositionDecisionResultAggregator aggregator;

    @Mock
    protected SjpService sjpService;

    private final UUID defendantId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID offenceId1 = randomUUID();
    private final UUID offenceId2 = randomUUID();
    private final UUID decisionId = randomUUID();

    public FinancialImpositionDecisionResultAggregatorTest() {
    }

    @Before
    public void setUp() {
        super.setUp();
        aggregator = new FinancialImpositionDecisionResultAggregator(jCachedReferenceData, sjpService);
    }

    @Test
    public void shouldPopulateCostsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(150), null, new BigDecimal(30), null, null, true),
                new Payment(new BigDecimal(680), PAY_TO_COURT, null, null, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(caseId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("76d43772-0660-4a33-b5c6-8f8ccaf6b4e3"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Costs\n" +
                                "Amount of costs £150.00\nMajor creditor Television Licensing Organisation")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("db261fd9-c6bb-4e10-b93f-9fd98418f7b0"))),
                                hasProperty("value", Matchers.is("£150.00")))
                        )))))));
    }

    @Test
    public void shouldPopulateReasonForNoCostsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(30), null, null, true),
                new Payment(new BigDecimal(680), PAY_TO_COURT, null, null, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(caseId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("baf94928-04ae-4609-8e96-efc9f081b2be"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("No order for costs\nReason for no costs already paid")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("be2a46db-709d-4e0d-9b63-aeb831564c1d"))),
                                hasProperty("value", Matchers.is("already paid")))
                        )))))));
    }

    @Test
    public void shouldPopulateCollectionOrderResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(30), null, null, true),
                new Payment(new BigDecimal(680), PAY_TO_COURT, "other payment", null, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("9ea0d845-5096-44f6-9ce0-8ae801141eac"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Collection order\n" +
                                "Collection order type Make payments as ordered\n" +
                                "Reason for not making an attachment of earnings order or deduction from benefit application other payment")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("6b36e5ff-e116-4dc3-b438-8c02d493959e"))),
                                        hasProperty("value", Matchers.is("Make payments as ordered")))
                                ), hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("369b6e22-4678-4b04-9fe9-5bb53bed5067"))),
                                        hasProperty("value", Matchers.is("other payment"))))))))));
    }

    @Test
    public void shouldPopulateDeductFromBenefitsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(30), null, null, true),
                new Payment(new BigDecimal(680), DEDUCT_FROM_BENEFITS, "other payment", ReasonForDeductingFromBenefits.DEFENDANT_KNOWN_DEFAULTER, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("f7dfefd2-64c6-11e8-adc0-fa7ae01bbebc"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Application made for benefit deductions" +
                                "\nReason defendant is a known defaulter")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("8273d5ba-680e-11e8-adc0-fa7ae01bbebc"))),
                                        hasProperty("value", Matchers.is("defendant is a known defaulter")))
                                )))))));
    }


    @Test
    public void shouldPopulateAttachToEarningsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(30), null, null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("bdb32555-8d55-4dc1-b4b6-580db5132496"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Attachment of earnings order\nReason defendant requested this")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("a289b1bd-06c8-4da3-b117-0bae6017857c"))),
                                        hasProperty("value", Matchers.is("defendant requested this")))
                                )))))));
    }


    @Test
    public void shouldPopulateVictimSurchargeResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(30), null, null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("e866cd11-6073-4fdf-a229-51c9d694e1d0"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Surcharge\nAmount of surcharge £30.00")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("629a971e-9d7a-4526-838d-0a4cb922b5cb"))),
                                        hasProperty("value", Matchers.is("£30.00")))
                                )))))));
    }

    @Test
    public void shouldPopulateNoVictimSurchargeReasonResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(0), "already paid", null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("204fc6b8-d6c9-4fb8-acd0-47d23c087625"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("No surcharge\nReason for no surcharge already paid")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("042742a1-8d47-4558-9b3e-9f34b358e034"))),
                                        hasProperty("value", Matchers.is("already paid")))
                                )))))));
    }

    @Test
    public void shouldPopulateLumSumInstallmentsWithReservedTermsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(0), "already paid", null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        new Installments(new BigDecimal(60), InstallmentPeriod.MONTHLY, LocalDate.of(2021, 9, 1))
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("d6e93aae-5dd7-11e8-9c2d-fa7ae01bbebc"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Reserve Terms Lump sum plus instalments\n" +
                                "Lump sum amount £680.00\n" +
                                "Instalment amount £60.00\n" +
                                "Instalment start date 01/09/2021\n" +
                                "Payment frequency monthly")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("8e235a65-5ea2-4fff-ba3b-6cdb74195436"))),
                                        hasProperty("value", Matchers.is("£680.00")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"))),
                                        hasProperty("value", Matchers.is("£60.00")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"))),
                                        hasProperty("value", Matchers.is("01/09/2021")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"))),
                                        hasProperty("value", Matchers.is("monthly")))
                                )
                        ))))));
    }

    @Test
    public void shouldPopulateLumSumInstallmentsWithNoReservedTermsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(0), "already paid", null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        false,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        new Installments(new BigDecimal(60), InstallmentPeriod.MONTHLY, LocalDate.of(2021, 9, 1))
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("272d1ec2-634b-11e8-adc0-fa7ae01bbebc"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Payment terms - Lump sum plus instalments\n" +
                                "Lump sum amount £680.00\n" +
                                "Instalment amount £60.00\n" +
                                "Instalment start date 01/09/2021\n" +
                                "Payment frequency following lump sum monthly")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("8e235a65-5ea2-4fff-ba3b-6cdb74195436"))),
                                        hasProperty("value", Matchers.is("£680.00")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"))),
                                        hasProperty("value", Matchers.is("£60.00")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"))),
                                        hasProperty("value", Matchers.is("01/09/2021")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("fb4f761c-29d0-4a8e-a947-3debf281dab0"))),
                                        hasProperty("value", Matchers.is("monthly")))
                                )
                        ))))));
    }

    @Test
    public void shouldPopulateLumSumWithReservedTermsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(0), "already paid", null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 14, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("a09bbfa0-5dd5-11e8-9c2d-fa7ae01bbebc"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Reserve Terms Lump sum\nPay lump sum in full within lump sum 14 days")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("c131cab0-5dd6-11e8-9c2d-fa7ae01bbebc"))),
                                        hasProperty("value", Matchers.is("lump sum 14 days")))
                                )
                        ))))));
    }

    @Test
    public void shouldPopulateLumSumWithNoReservedTermsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(0), "already paid", null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        false,
                        new LumpSum(new BigDecimal(680), 28, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("bcb5a496-f7cf-11e8-8eb2-f2801f1b9fd1"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Pay by date\nDate to pay in full by 28/05/2021")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("ee7d253a-c629-11e8-a355-529269fb1459"))),
                                        hasProperty("value", Matchers.is("28/05/2021")))
                                )
                        ))))));
    }

    @Test
    public void shouldPopulateInstallmentsWithReservedTermsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(0), "already paid", null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        true,
                        null,
                        new Installments(new BigDecimal(60), InstallmentPeriod.MONTHLY, LocalDate.of(2021, 9, 1))
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("9ba8f03a-5dda-11e8-9c2d-fa7ae01bbebc"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Reserve Terms Instalments only\nInstalment amount £60.00\nInstalment start date 01/09/2021\nPayment frequency monthly")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"))),
                                        hasProperty("value", Matchers.is("£60.00")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"))),
                                        hasProperty("value", Matchers.is("01/09/2021")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"))),
                                        hasProperty("value", Matchers.is("monthly")))
                                )
                        ))))));
    }

    @Test
    public void shouldPopulateInstallmentsWithNoReservedTermsResult() {

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(null, "already paid", new BigDecimal(0), "already paid", null, true),
                new Payment(new BigDecimal(680), ATTACH_TO_EARNINGS, null, ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED, new PaymentTerms(
                        false,
                        null,
                        new Installments(new BigDecimal(60), InstallmentPeriod.MONTHLY, LocalDate.of(2021, 9, 1))
                ), null)
        );

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, now(), offenceDecisions, financialImposition, null);

        aggregator.aggregate(decisionSaved, sjpSessionEnvelope, resultsAggregate, defendantId, caseId, resultedOn, "TVL");

        assertThat(resultsAggregate.getResults(defendantId), allOf(
                hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("6d76b10c-64c4-11e8-adc0-fa7ae01bbebc"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Payment terms - Instalments only\nInstalment amount £60.00\nInstalment start date 01/09/2021\nPayment frequency monthly")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("1393acda-7a35-4d65-859d-6298e1470cf1"))),
                                        hasProperty("value", Matchers.is("£60.00")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("b487696e-dfc9-4c89-80d3-337a4319e925"))),
                                        hasProperty("value", Matchers.is("01/09/2021")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("f2a61e80-c13e-4f44-8e91-8ce23e85596b"))),
                                        hasProperty("value", Matchers.is("monthly")))
                                )
                        ))))));
    }
}
