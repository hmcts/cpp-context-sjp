package uk.gov.moj.cpp.sjp.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.DeductingFromBenefitsReason.COMPENSATION_ORDERED;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod.MONTHLY;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.DEDUCT_FROM_BENEFITS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.api.service.CaseService;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.DeductingFromBenefitsReason;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DecisionApiTest {

    private static final String SAVE_DECISION_COMMAND_NAME = "sjp.save-decision";
    private static final String SAVE_APPLICATION_DECISION_COMMAND_NAME = "sjp.save-application-decision";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private CaseService caseService;

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now());

    @InjectMocks
    private DecisionApi decisionApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    private static final UUID OFFENCE1_ID = randomUUID();
    private static final UUID OFFENCE2_ID = randomUUID();
    private static final BigDecimal MAX_FINE_VALUE= BigDecimal.valueOf(999999999.99);

    final JsonObject caseDetails = createObjectBuilder().add("defendant",
            createObjectBuilder().add("offences",
                    createArrayBuilder()
                            .add(createObjectBuilder().add("id", OFFENCE1_ID.toString())
                                    .add("maxFineLevel", 5)
                                    .add("maxFineValue", MAX_FINE_VALUE))
                            .add(createObjectBuilder().add("id", OFFENCE2_ID.toString())
                                    .add("maxFineLevel", 5)
                                    .add("maxFineValue", MAX_FINE_VALUE)))).build();


    @Test
    public void shouldHandleDecisionCommands() {
        assertThat(DecisionApi.class, isHandlerClass(COMMAND_API)
                .with(method("saveDecision").thatHandles(SAVE_DECISION_COMMAND_NAME))
                .with(method("saveApplicationDecision").thatHandles(SAVE_APPLICATION_DECISION_COMMAND_NAME))
        );
    }

    @Test
    public void shouldEnrichCommandWithSavedAtForWithdrawDecision() {
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithWithdrawDecision());
        decisionApi.saveDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());
        verifyDecisionEnriched(envelope);
    }

    @Test
    public void shouldThrowBadRequestWhenAdjournDateIsInThePast() {
        final LocalDate yesterday = now().minusDays(1);
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithAdjournDecisionWithAdjournDate(yesterday));
        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("The adjournment date must be between 1 and 28 days in the future"));
    }

    @Test
    public void shouldThrowBadRequestWhenAdjournDateIsMoreThan28DaysInFuture() {
        // assume no exception thrown up to 28 days
        final LocalDate dateWithinUpperLimit = now().plusDays(28);
        final JsonEnvelope validEnvelope = createCaseDecisionCommandWithOffence(buildOffenceWithAdjournDecisionWithAdjournDate(dateWithinUpperLimit));
        decisionApi.saveDecision(validEnvelope);

        final LocalDate futureAdjournDate = now().plusDays(29);
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithAdjournDecisionWithAdjournDate(futureAdjournDate));
        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("The adjournment date must be between 1 and 28 days in the future"));
        verify(sender).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldThrowBadRequestWhenAdjournDateIsSameAsToday() {
        // assume no exception thrown with date within lower limit (D+1)
        final LocalDate tomorrow = now().plusDays(1);
        final JsonEnvelope validEnvelope = createCaseDecisionCommandWithOffence(buildOffenceWithAdjournDecisionWithAdjournDate(tomorrow));
        decisionApi.saveDecision(validEnvelope);

        final LocalDate adjournDate = now();
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithAdjournDecisionWithAdjournDate(adjournDate));
        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("The adjournment date must be between 1 and 28 days in the future"));
        verify(sender).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldEnrichCommandForAdjournDecision() {
        final LocalDate futureAdjournDate = now().plusDays(10);
        JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithAdjournDecisionWithAdjournDate(futureAdjournDate));
        decisionApi.saveDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());
        verifyDecisionEnriched(envelope);
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_WithoutFinancialImposition() {
        var e = assertThrows(BadRequestException.class, () -> saveDecision(null));
        assertThat(e.getMessage(), is("Financial imposition is required for discharge or financial penalty decisions"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_PayToCourt_NoReasonWhyNotAttachedOrDeducted() {
        final JsonObject payment = buildPayment(PAY_TO_COURT, null, buildPaymentTerms(), null);
        JsonObject costsAndSurcharge = buildCostsAndSurcharge();
        JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, payment);
        var e = assertThrows(BadRequestException.class, () -> saveDecision(financialImposition));
        assertThat(e.getMessage(), is("reasonWhyNotAttachedOrDeducted is required if paymentType is pay to court"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_PayToCourt_ReserveTerms_True() {
        final JsonObject paymentTerms = buildPaymentTerms(true, buildLumpSum(), buildInstallments());
        JsonObject payment = buildPayment(paymentTerms);
        JsonObject costsAndSurcharge = buildCostsAndSurcharge();
        JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, payment);
        var e = assertThrows(BadRequestException.class, () -> saveDecision(financialImposition));
        assertThat(e.getMessage(), is("reserveTerms must be false if paymentType is pay to court"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_DeductFromBenefits_NoDeductingFromBenefitsReason() {
        final JsonObject paymentTerms = buildPaymentTerms(true, buildLumpSum(), buildInstallments());
        final JsonObject payment = buildPayment(DEDUCT_FROM_BENEFITS, null, paymentTerms, null);
        JsonObject costsAndSurcharge = buildCostsAndSurcharge();
        JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, payment);
        var e = assertThrows(BadRequestException.class, () -> saveDecision(financialImposition));
        assertThat(e.getMessage(), is("reasonForDeductingFromBenefits is required if paymentType is deduct from benefits"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_DeductFromBenefits_ReserveTerms_False() {
        final JsonObject paymentTerms = buildPaymentTerms(false, null, null);
        final JsonObject payment = buildPayment(DEDUCT_FROM_BENEFITS, null, paymentTerms, COMPENSATION_ORDERED);
        JsonObject costsAndSurcharge = buildCostsAndSurcharge();
        JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, payment);
        var e = assertThrows(BadRequestException.class, () -> saveDecision(financialImposition));
        assertThat(e.getMessage(), is("reserveTerms must be true if paymentType is deduct from benefits"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_Without_LumpSum_Without_Installments() {
        final JsonObject paymentTerms = buildPaymentTerms(false, null, null);
        JsonObject payment = buildPayment(paymentTerms);
        JsonObject costsAndSurcharge = buildCostsAndSurcharge();
        JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, payment);
        var e = assertThrows(BadRequestException.class, () -> saveDecision(financialImposition));
        assertThat(e.getMessage(), is("Either lump sum or installments is required"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_LumpSum_Without_WithinDays_Without_PayByDate() {
        final JsonObject lumpSum = buildLumpSum(null, null);
        final JsonObject paymentTerms = buildPaymentTerms(false, lumpSum, null);
        JsonObject payment = buildPayment(paymentTerms);
        JsonObject costsAndSurcharge = buildCostsAndSurcharge();
        JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, payment);
        var e = assertThrows(BadRequestException.class, () -> saveDecision(financialImposition));
        assertThat(e.getMessage(), is("Either withinDays or payByDate is required"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_With_ZeroVictimSurcharge_NoReasonForNoVictimSurcharge() {
        final JsonObject costsAndSurcharge = buildCostsAndSurcharge(BigDecimal.TEN, null, ZERO, null);
        final JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, buildPayment());
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithDischarge(CONDITIONAL, BigDecimal.TEN, null, buildDischargedFor()), financialImposition);
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);
        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("reasonForNoVictimSurcharge is required"));
    }

    @Test
    public void shouldThrowBadRequestWhen_Discharge_Conditional_Without_DischargedFor() {
        final JsonObject costsAndSurcharge = buildCostsAndSurcharge(BigDecimal.TEN, null, BigDecimal.TEN, null);
        final JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, buildPayment());
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithDischarge(CONDITIONAL, BigDecimal.TEN, null, null), financialImposition);
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);
        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("dischargedFor is required for conditional discharge"));
    }

    @Test
    public void shouldPassValidationWhen_Discharge_Conditional_With_Excise_Penalty() {
        final JsonArray offences = buildOffenceWithDischargeAndExcisePenalty(CONDITIONAL, BigDecimal.TEN, null, buildDischargedFor());
        final JsonObject caseDetails = createObjectBuilder().add("defendant",
                createObjectBuilder().add("offences",
                        createArrayBuilder()
                                .add(createObjectBuilder().add("id", offences.getJsonObject(0).getString("offenceId"))
                                        .add("penaltyType", "Excise penalty")
                                        .add("maxFineLevel", 5)
                                        .add("maxFineValue", MAX_FINE_VALUE)))).build();

        final JsonObject costsAndSurcharge = buildCostsAndSurcharge(BigDecimal.TEN, null, null, null);
        final JsonObject financialImposition = buildFinancialImposition(costsAndSurcharge, buildPayment());
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(offences, financialImposition);
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);
        decisionApi.saveDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldThrowBadRequestWhen_FinancialPenalty_Compensation_And_Fine_Not_Provided() {
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithFinancialPenaltyDecision(), buildDefaultFinancialImposition());

        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);

        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("Both compensation and fine cannot be empty"));
    }

    @Test
    public void shouldPassValidationWhen_FinancialPenalty_Compensation_Is_Provided() {
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithFinancialPenaltyDecisionWithCompensation(), buildDefaultFinancialImposition());
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);
        decisionApi.saveDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().payload().toString(), isJson(
                withJsonPath("$.offenceDecisions[0]", isJson(allOf(
                        withJsonPath("type", is("FINANCIAL_PENALTY")),
                        withJsonPath("compensation", is(10))
                )))));
    }

    @Test
    public void shouldPassValidationWhen_FinancialPenalty_Fine_Is_Provided_And_Compensation_Reason_Should_Be_Optional() {
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithFinancialPenaltyDecisionFineAndWithoutCompensationReason(), buildDefaultFinancialImposition());
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);
        decisionApi.saveDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().payload().toString(), isJson(
                withJsonPath("$.offenceDecisions[0]", isJson(allOf(
                        withJsonPath("type", is("FINANCIAL_PENALTY")),
                        withJsonPath("fine", is(10))
                )))));
    }

    @Test
    public void shouldThrowBadRequestWhen_FinancialPenalty_WithoutFinancialImposition() {
        JsonArray offenceDecisions = buildOffenceWithFinancialPenaltyDecisionWithCompensation();
        var e = assertThrows(BadRequestException.class, () -> saveDecision(offenceDecisions, null));
        assertThat(e.getMessage(), is("Financial imposition is required for discharge or financial penalty decisions"));
    }

    @Test
    public void shouldThrowBadRequestWhen_FinancialPenalty_ExcisePenalty_Is_Greater_Than_Max_Value() {
        final JsonObject caseDetails = createObjectBuilder().add("defendant",
                createObjectBuilder().add("offences",
                        createArrayBuilder()
                                .add(createObjectBuilder().add("id", OFFENCE1_ID.toString())
                                        .add("maxFineLevel", 5)
                                        .add("maxFineValue", MAX_FINE_VALUE))
                                .add(createObjectBuilder().add("id", OFFENCE2_ID.toString())
                                        .add("maxFineLevel", 5)
                                        .add("maxFineValue", MAX_FINE_VALUE)))).build();


        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithFinancialPenaltyDecisionWithExcisePenalty());
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);

        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("The maximum excise penalty for this offence is £" + MAX_FINE_VALUE + ""));
    }

    @Test
    public void shouldThrowBadRequestWhen_FinancialPenalty_Fine_Is_Greater_Than_Max_Fine_Value_When_FineLevel_Less_Than_Five() {
        final JsonObject aCase = createObjectBuilder().add("defendant",
                createObjectBuilder().add("offences",
                        createArrayBuilder()
                                .add(createObjectBuilder().add("id", OFFENCE1_ID.toString())
                                        .add("maxFineLevel", 2)
                                        .add("maxFineValue", 9))
                                .add(createObjectBuilder().add("id", OFFENCE2_ID.toString())
                                        .add("maxFineLevel", 2)
                                        .add("maxFineValue", 9)))).build();

        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithFinancialPenaltyDecisionFineWithFineLevelLessThanFive(), buildDefaultFinancialImposition());
        when(caseService.getCaseDetails(envelope)).thenReturn(aCase);

        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveDecision(envelope));
        assertThat(e.getMessage(), is("The maximum fine for this offence is £9"));
    }

    @Test
    public void shouldSaveDecision_FinancialPenalty_When_FineLevel_Is_Five() {
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(buildOffenceWithFinancialPenaltyDecisionFineWithFineLevelFive(), buildDefaultFinancialImposition());
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);

        decisionApi.saveDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldThrowBadRequestWhenApplicationRejectedWithoutReason() {
        final JsonEnvelope envelope = createApplicationDecisionCommand(false, null, false, null);
        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveApplicationDecision(envelope));
        assertThat(e.getMessage(), is("Rejection reason must be provided if application is rejected"));
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldThrowBadRequestWhenApplicationOutOfTimeWithoutReason() {
        final JsonEnvelope envelope = createApplicationDecisionCommand(true, null, true, null);
        var e = assertThrows(BadRequestException.class, () -> decisionApi.saveApplicationDecision(envelope));
        assertThat(e.getMessage(), is("Out of time reason must be provided if application is out of time"));
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldSaveApplicationDecision() {
        final JsonEnvelope envelope = createApplicationDecisionCommand(true, null, false, null);
        decisionApi.saveApplicationDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata().name(), is("sjp.command.controller.save-application-decision"));
        assertThat(sentEnvelope.payload().toString(), isJson(
                allOf(
                    withJsonPath("$.granted", is(true)),
                    withJsonPath("$.outOfTime", is(false))
                )
        ));
    }

    @Test
    public void shouldCaptureCourtScheduleIdInReferForCourtHearingDecision() {
        final UUID courtScheduleId = randomUUID();
        final JsonObject nextHearing = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("courtId", "1234")
                .add("dateAndTime", ZonedDateTime.now(clock.now().getZone()).toString())
                .add("courtScheduleId", courtScheduleId.toString())
                .build();

        final JsonObject referDecision = createObjectBuilder()
                .add("offenceId", OFFENCE1_ID.toString())
                .add("type", "REFER_FOR_COURT_HEARING")
                .add("nextHearing", nextHearing)
                .build();

        final JsonArray offenceDecisions = createArrayBuilder().add(referDecision).build();
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(offenceDecisions);

        decisionApi.saveDecision(envelope);

        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().payload().toString(), isJson(
                withJsonPath("$.offenceDecisions[0].nextHearing.courtScheduleId", is(courtScheduleId.toString()))
        ));
    }


    @Test
    void shouldCompleteCaseViaBdf() {
        final UUID caseId = randomUUID();
        final JsonEnvelope envelope = createCaseCompleteBdfPayload(caseId);
        decisionApi.caseCompleteBdf(envelope);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata().name(), is("sjp.command.case-complete-bdf"));
        assertThat(sentEnvelope.payload().toString(), isJson(
                allOf(
                        withJsonPath("$.caseId", is(caseId.toString()))
                )
        ));
    }

    private JsonEnvelope createCaseCompleteBdfPayload(final UUID caseId) {
        final JsonObjectBuilder applicationBuilder = createObjectBuilder()
                .add("caseId", caseId.toString());
        return envelopeFrom(metadataWithRandomUUID("sjp.command.case-complete-bdf"), applicationBuilder.build());
    }

    private JsonEnvelope createApplicationDecisionCommand(final boolean granted, final String rejectionReason,
                                                          final boolean outOfTime, final String outOfTimeReason) {

        final JsonObjectBuilder applicationBuilder = createObjectBuilder()
                .add("granted", granted)
                .add("outOfTime", outOfTime);

        ofNullable(rejectionReason).ifPresent(reason -> applicationBuilder.add("rejectionReason", reason));
        ofNullable(outOfTimeReason).ifPresent(reason -> applicationBuilder.add("outOfTimeReason", reason));

        return envelopeFrom(metadataWithRandomUUID(SAVE_APPLICATION_DECISION_COMMAND_NAME), applicationBuilder.build());
    }

    private void saveDecision(final JsonObject financialImposition) {
        saveDecision(buildOffenceWithDischarge(), financialImposition);
    }

    private void saveDecision(final JsonArray offenceDecisions, final JsonObject financialImposition) {
        final JsonEnvelope envelope = createCaseDecisionCommandWithOffence(offenceDecisions, financialImposition);
        when(caseService.getCaseDetails(envelope)).thenReturn(caseDetails);
        decisionApi.saveDecision(envelope);
        verify(sender).send(envelopeCaptor.capture());
    }

    private JsonObject buildLumpSum() {
        return buildLumpSum(14, null);
    }

    private JsonObject buildLumpSum(final Integer withinDays, final LocalDate payByDate) {
        final JsonObjectBuilder lumpSumBuilder = createObjectBuilder();
        lumpSumBuilder.add("amount", 100);
        if (withinDays != null) {
            lumpSumBuilder.add("withinDays", withinDays);
        }
        if (payByDate != null) {
            lumpSumBuilder.add("payByDate", payByDate.toString());
        }

        return lumpSumBuilder.build();
    }

    private JsonObject buildInstallments() {
        return createObjectBuilder()
                .add("amount", 1234)
                .add("period", MONTHLY.name())
                .add("startDate", "2020-01-01")
                .build();
    }

    private JsonObject buildPaymentTerms() {
        return buildPaymentTerms(false, buildLumpSum(), null);
    }

    private JsonObject buildPaymentTerms(final Boolean reserveTerms, final JsonObject lumpSum, final JsonObject installments) {
        final JsonObjectBuilder paymentTermsBuilder = createObjectBuilder();
        paymentTermsBuilder.add("reserveTerms", reserveTerms);
        if (lumpSum != null) {
            paymentTermsBuilder.add("lumpSum", lumpSum);
        }
        if (installments != null) {
            paymentTermsBuilder.add("installments", installments);
        }
        return paymentTermsBuilder.build();
    }

    private JsonEnvelope createCaseDecisionCommandWithOffence(final JsonArray offenceDecisions) {
        return createCaseDecisionCommandWithOffence(offenceDecisions, null);
    }

    private void verifyDecisionEnriched(JsonEnvelope envelope) {
        JsonObjectBuilder expected = uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder(envelope.payloadAsJsonObject());
        expected.add("savedAt", clock.now().toString());
        assertThat(envelopeCaptor.getValue().payload(), equalTo(expected.build()));
    }

    private JsonEnvelope createCaseDecisionCommandWithOffence(final JsonArray offenceDecisions, final JsonObject financialImposition) {
        final JsonObjectBuilder caseDecisionCommandBuilder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("decisionId", randomUUID().toString())
                .add("sessionId", randomUUID().toString())
                .add("note", "wrongly convicted")
                .add("offenceDecisions", offenceDecisions);
        if (financialImposition != null) {
            caseDecisionCommandBuilder.add("financialImposition", financialImposition);
        }
        return envelopeFrom(metadataWithRandomUUID("sjp.command.controller.save-decision"), caseDecisionCommandBuilder.build());
    }


    private static JsonArray buildOffenceWithWithdrawDecision() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceId", randomUUID().toString())
                        .add("type", WITHDRAW.toString())
                        .add("withdrawalReasonId", randomUUID().toString()))
                .add(createObjectBuilder()
                        .add("offenceId", randomUUID().toString())
                        .add("type", WITHDRAW.toString())
                        .add("withdrawalReasonId", randomUUID().toString()))
                .build();
    }

    private static JsonArray buildOffenceWithAdjournDecisionWithAdjournDate(final LocalDate adjournTo) {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceId", randomUUID().toString())
                        .add("type", WITHDRAW.toString())
                        .add("withdrawalReasonId", randomUUID().toString()))
                .add(createObjectBuilder()
                        .add("offenceId", randomUUID().toString())
                        .add("type", ADJOURN.toString())
                        .add("reason", "Not enough document for decision")
                        .add("adjournTo", adjournTo.toString()))
                .build();
    }

    private static JsonArray buildOffenceWithDischarge() {
        return buildOffenceWithDischarge(ABSOLUTE, BigDecimal.TEN, null, null);
    }

    private static JsonArray buildOffenceWithDischarge(final DischargeType dischargeType, final BigDecimal compensation, final String noCompensationReason, final JsonObject dischargedFor) {
        final JsonObjectBuilder decisionBuilder = createObjectBuilder()
                .add("offenceId", OFFENCE1_ID.toString())
                .add("type", DISCHARGE.toString())
                .add("verdict", PROVED_SJP.name())
                .add("guiltyPleaTakenIntoAccount", true);
        if (compensation != null) {
            decisionBuilder.add("compensation", compensation);
        }

        if (!StringUtils.isBlank(noCompensationReason)) {
            decisionBuilder.add("noCompensationReason", noCompensationReason);
        }

        if (dischargedFor != null) {
            decisionBuilder.add("dischargedFor", dischargedFor);
        }

        if (dischargeType != null) {
            decisionBuilder.add("dischargeType", dischargeType.name());
        }

        return createArrayBuilder()
                .add(decisionBuilder.build())
                .build();
    }

    private static JsonArray buildOffenceWithDischargeAndExcisePenalty(final DischargeType dischargeType, final BigDecimal compensation, final String noCompensationReason, final JsonObject dischargedFor) {
        final JsonObjectBuilder decisionBuilder = createObjectBuilder()
                .add("offenceId", randomUUID().toString())
                .add("type", DISCHARGE.toString())
                .add("verdict", PROVED_SJP.name())
                .add("guiltyPleaTakenIntoAccount", true)
                .add("excisePenalty", ONE);
        if (compensation != null) {
            decisionBuilder.add("compensation", compensation);
        }

        if (!StringUtils.isBlank(noCompensationReason)) {
            decisionBuilder.add("noCompensationReason", noCompensationReason);
        }

        if (dischargedFor != null) {
            decisionBuilder.add("dischargedFor", dischargedFor);
        }

        if (dischargeType != null) {
            decisionBuilder.add("dischargeType", dischargeType.name());
        }

        return createArrayBuilder()
                .add(decisionBuilder.build())
                .build();
    }

    private JsonObject buildFinancialImposition(final JsonObject costsAndSurcharge, final JsonObject payment) {
        return createObjectBuilder()
                .add("costsAndSurcharge", costsAndSurcharge)
                .add("payment", payment)
                .build();
    }

    private JsonObject buildDefaultFinancialImposition() {
        return createObjectBuilder()
                .add("costsAndSurcharge", buildCostsAndSurcharge())
                .add("payment", buildPayment())
                .build();
    }

    private JsonObject buildCostsAndSurcharge() {
        return buildCostsAndSurcharge(BigDecimal.TEN, null, ONE, null);
    }

    private JsonObject buildCostsAndSurcharge(final BigDecimal costs, final String reasonForNoCosts, final BigDecimal victimSurcharge, final String reasonForNoVictimSurcharge) {
        final JsonObjectBuilder costsAndSurchargeBuilder = createObjectBuilder()
                .add("costs", costs)
                .add("collectionOrderMade", true);
        if(nonNull(victimSurcharge)) {
            costsAndSurchargeBuilder.add("victimSurcharge", victimSurcharge);
        }
        if (!StringUtils.isBlank(reasonForNoCosts)) {
            costsAndSurchargeBuilder.add("reasonForNoCosts", reasonForNoCosts);
        }
        if (!StringUtils.isBlank(reasonForNoVictimSurcharge)) {
            costsAndSurchargeBuilder.add("reasonForNoVictimSurcharge", reasonForNoVictimSurcharge);
        }
        return costsAndSurchargeBuilder.build();
    }

    private JsonObject buildPayment() {
        return buildPayment(PAY_TO_COURT, "Some reason", buildPaymentTerms(), null);
    }

    private JsonObject buildPayment(final JsonObject paymentTerms) {
        return buildPayment(PAY_TO_COURT, "Some reason", paymentTerms, null);
    }

    private JsonObject buildPayment(final PaymentType paymentType, final String reasonWhyNotAttachedOrDeducted, final JsonObject paymentTerms, final DeductingFromBenefitsReason deductingFromBenefitsReason) {
        final JsonObjectBuilder paymentBuilder = createObjectBuilder()
                .add("totalSum", 1234)
                .add("paymentType", paymentType.name())
                .add("collectionOrderMade", true)
                .add("paymentTerms", paymentTerms);
        if (!StringUtils.isBlank(reasonWhyNotAttachedOrDeducted)) {
            paymentBuilder.add("reasonWhyNotAttachedOrDeducted", reasonWhyNotAttachedOrDeducted);
        }
        if (deductingFromBenefitsReason != null) {
            paymentBuilder.add("reasonForDeductingFromBenefits", deductingFromBenefitsReason.name());
        }
        return paymentBuilder.build();
    }

    private static JsonArray buildOffenceWithFinancialPenaltyDecision() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE1_ID.toString()))
                        .add("offenceId", OFFENCE1_ID.toString())
                        .add("fine", ZERO)
                        .add("type", FINANCIAL_PENALTY.toString()))
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE2_ID.toString()))
                        .add("offenceId", OFFENCE2_ID.toString())
                        .add("fine", ZERO)
                        .add("compensation", ZERO)
                        .add("type", FINANCIAL_PENALTY.toString()))
                .build();
    }

    private static JsonArray buildOffenceWithFinancialPenaltyDecisionWithCompensation() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE1_ID.toString()))
                        .add("offenceId", OFFENCE1_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("compensation", 10))
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE2_ID.toString()))
                        .add("offenceId", OFFENCE2_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("compensation", 20))
                .build();
    }

    private static JsonArray buildOffenceWithFinancialPenaltyDecisionWithExcisePenalty() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE1_ID.toString()))
                        .add("offenceId", OFFENCE1_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("excisePenalty", MAX_FINE_VALUE.add(ONE)))
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE2_ID.toString()))
                        .add("offenceId", OFFENCE2_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("excisePenalty", MAX_FINE_VALUE))
                .build();
    }

    private static JsonArray buildOffenceWithFinancialPenaltyDecisionFineAndWithoutCompensationReason() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE1_ID.toString()))
                        .add("offenceId", OFFENCE1_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("fine", 10))
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE2_ID.toString()))
                        .add("offenceId", OFFENCE2_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("fine", 20))
                .build();
    }

    private static JsonArray buildOffenceWithFinancialPenaltyDecisionFineWithFineLevelLessThanFive() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE1_ID.toString()))
                        .add("offenceId", OFFENCE1_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("fine", 10))
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE2_ID.toString()))
                        .add("offenceId", OFFENCE2_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("fine", 20))
                .build();
    }

    private static JsonArray buildOffenceWithFinancialPenaltyDecisionFineWithFineLevelFive() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE1_ID.toString()))
                        .add("offenceId", OFFENCE1_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("fine", 10))
                .add(createObjectBuilder()
                        .add("offenceDecisionInformation", createObjectBuilder().add("offenceId", OFFENCE2_ID.toString()))
                        .add("offenceId", OFFENCE2_ID.toString())
                        .add("type", FINANCIAL_PENALTY.toString())
                        .add("fine", 20))
                .build();
    }

    private JsonObject buildDischargedFor() {
        return createObjectBuilder()
                .add("value", 12)
                .add("unit", PeriodUnit.MONTH.name())
                .build();
    }
}
