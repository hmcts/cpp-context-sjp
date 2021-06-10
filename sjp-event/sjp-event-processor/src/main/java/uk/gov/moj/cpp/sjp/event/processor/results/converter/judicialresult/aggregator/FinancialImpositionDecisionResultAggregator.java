package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DEDUCT_FROM_BENEFITS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.FOURTEEN_DAYS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.TWENTY_EIGHT_DAYS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.AEOC_REASON;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.AMOUNT_OF_COSTS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.AMOUNT_OF_SURCHARGE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.COLLECTION_ORDER_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYEE_REFERENCE_NO;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYER_ADDRESS_LINE_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYER_ADDRESS_LINE_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYER_ADDRESS_LINE_3;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYER_ADDRESS_LINE_4;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYER_ADDRESS_LINE_5;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYER_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.EMPLOYER_POSTCODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.FCOST_MAJOR_CREDITOR;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.INSTALMENTS_AMOUNT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.INSTL_INSTALMENT_START_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.INSTL_PAYMENT_FREQUENCY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LSUM_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LUMSI_INSTALMENT_AMOUNT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LUMSI_INSTALMENT_START_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LUMSI_LUMP_SUM_AMOUNT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LUMSI_PAYMENT_FREQUENCY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NCOSTS_REASON;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.PAY_WITHIN_DAYS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.REASON_FOR_NOT_IMPOSING_OR_REDUCING_VICTIM_SURCHARGE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.REASON_NOT_ABD_OR_AEO;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.REASON_OPTIONAL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.RINSTL_INSTALMENT_AMOUNT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.RINSTL_INSTALMENT_START_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.RINSTL_PAYMENT_FREQUENCY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.RLSUMI_INSTALMENT_AMOUNT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.RLSUMI_INSTALMENT_START_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.RLSUMI_LUMP_SUM_AMOUNT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.RLSUMI_PAYMENT_FREQUENCY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.ABDC;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.AEOC;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.COLLO;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.FCOST;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.FVS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.INSTL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LSUM;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LUMSI;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.NCOSTS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.NOVS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.RINSTL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.RLSUM;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.RLSUMI;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;

public class FinancialImpositionDecisionResultAggregator extends DecisionResultAggregator {

    private final SjpService sjpService;

    private static final Map<String, String> deductFromBenefitsMap = ImmutableMap.of(
            ReasonForDeductingFromBenefits.COMPENSATION_ORDERED.name(), "CO",
            ReasonForDeductingFromBenefits.DEFENDANT_KNOWN_DEFAULTER.name(), "KD",
            ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED.name(), "DR"
    );

    private static final Map<String, String> installmentPeriodMap = ImmutableMap.of(
            InstallmentPeriod.WEEKLY.name(), "WKLY",
            InstallmentPeriod.FORTNIGHTLY.name(), "FTNLY",
            InstallmentPeriod.MONTHLY.name(), "MTHLY"
    );

    private static final Map<String, String> paymentTypeMap = ImmutableMap.of(
            PaymentType.PAY_TO_COURT.name(), "PAY",
            PaymentType.DEDUCT_FROM_BENEFITS.name(), "DB",
            PaymentType.ATTACH_TO_EARNINGS.name(), "AEO"
    );

    @Inject
    public FinancialImpositionDecisionResultAggregator(final JCachedReferenceData cachedReferenceData,
                                                       final SjpService sjpService) {
        super(cachedReferenceData);
        this.sjpService = sjpService;
    }

    public void aggregate(final DecisionSaved decisionSaved,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final UUID defendantId,
                          final UUID caseId,
                          final ZonedDateTime resultedOn,
                          final String prosecutingAuthority) {

        final List<JudicialResult> judicialResults = new ArrayList<>();

        if (decisionSaved.getFinancialImposition() != null) {
            judicialResults.addAll(financialImposition(decisionSaved, sjpSessionEnvelope, defendantId, resultedOn, prosecutingAuthority));
        }

        // case level results
        decisionAggregate.putResults(caseId, judicialResults);
    }

    private List<JudicialResult> financialImposition(final DecisionSaved decisionSaved,
                                                     final JsonEnvelope sjpSessionEnvelope,
                                                     final UUID defendantId,
                                                     final ZonedDateTime resultedOn,
                                                     final String prosecutingAuthority) {

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final FinancialImposition financialImposition = decisionSaved.getFinancialImposition();
        final CostsAndSurcharge costsAndSurcharge = financialImposition.getCostsAndSurcharge();
        final Payment payment = financialImposition.getPayment();

        //
        final PaymentTerms paymentTerms = payment.getPaymentTerms();

        // costs
        judicialResults.addAll(costsResult(costsAndSurcharge, sjpSessionEnvelope, resultedOn, prosecutingAuthority));

        // reason for no costs
        judicialResults.addAll(reasonForNoCosts(costsAndSurcharge, sjpSessionEnvelope, resultedOn));

        // collection order
        judicialResults.addAll(collectionOrder(financialImposition, sjpSessionEnvelope, resultedOn));

        // deduct from benefits and attachment of earnings
        judicialResults.addAll(deductFromBenefits(financialImposition.getPayment(), sjpSessionEnvelope, resultedOn));
        judicialResults.addAll(attachToEarnings(financialImposition.getPayment(), sjpSessionEnvelope, defendantId, resultedOn));

        // victim surcharge
        if (nonNull(costsAndSurcharge.getVictimSurcharge())) {
            final BigDecimal victimSurchargeValue = costsAndSurcharge.getVictimSurcharge();
            if (victimSurchargeValue.compareTo(BigDecimal.valueOf(0L)) > 0) {
                judicialResults.addAll(victimSurcharge(victimSurchargeValue, sjpSessionEnvelope, resultedOn));
            } else {
                final String victimSurchargeReason = getNoVictimSurchargeReason(decisionSaved);
                judicialResults.addAll(noVictimSurcharge(sjpSessionEnvelope, resultedOn, victimSurchargeReason));
            }
        }

        judicialResults.addAll(paymentTerms(paymentTerms, sjpSessionEnvelope, resultedOn));

        return judicialResults;
    }

    private List<JudicialResult> costsResult(final CostsAndSurcharge costsAndSurcharge,
                                             final JsonEnvelope sjpSessionEnvelope,
                                             final ZonedDateTime resultedOn,
                                             final String prosecutingAuthority) {
        final UUID resultId = FCOST.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();

        if (nonNull(costsAndSurcharge.getCosts())
                && costsAndSurcharge.getCosts().doubleValue() > 0) {
            final JudicialResultPrompt judicialResultPrompt = getPrompt(AMOUNT_OF_COSTS, resultDefinition)
                    .withJudicialResultPromptTypeId(AMOUNT_OF_COSTS.getId())
                    .withValue(getCurrencyAmount(costsAndSurcharge.getCosts().toString()))
                    .build();
            resultPrompts.add(judicialResultPrompt);

            jCachedReferenceData.getCreditorName(prosecutingAuthority, sjpSessionEnvelope)
                    .ifPresent(value -> resultPrompts.add(getPrompt(FCOST_MAJOR_CREDITOR, resultDefinition)
                            .withJudicialResultPromptTypeId(FCOST_MAJOR_CREDITOR.getId())
                            .withValue(value)
                            .build()));

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                            .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                            .build());
        }
        return judicialResults;
    }

    private List<JudicialResult> reasonForNoCosts(final CostsAndSurcharge costsAndSurcharge,
                                                  final JsonEnvelope sjpSessionEnvelope,
                                                  final ZonedDateTime resultedOn) {
        final UUID resultId = NCOSTS.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();

        if (costsAndSurcharge.getReasonForNoCosts() != null) {
            final JudicialResultPrompt nCostsReason = getPrompt(NCOSTS_REASON, resultDefinition)
                    .withJudicialResultPromptTypeId(NCOSTS_REASON.getId())
                    .withValue(costsAndSurcharge.getReasonForNoCosts())
                    .build();
            resultPrompts.add(nCostsReason);

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                            .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                            .build());
        }

        return judicialResults;
    }

    private List<JudicialResult> deductFromBenefits(final Payment payment,
                                                    final JsonEnvelope sjpSessionEnvelope,
                                                    final ZonedDateTime resultedOn) {
        final UUID resultId = ABDC.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();
        final PaymentType paymentType = payment.getPaymentType();

        if (paymentType != null
                && paymentType.name().equals(DEDUCT_FROM_BENEFITS)) {
            final JudicialResultPrompt reasonWhyNotAttached =
                    getPrompt(REASON_OPTIONAL, resultDefinition)
                            .withJudicialResultPromptTypeId(REASON_OPTIONAL.getId())
                            .withValue(jCachedReferenceData.getDeductFromFundsReason(deductFromBenefitsMap.get(payment.getReasonForDeductingFromBenefits().name()),
                                    envelopeFrom(sjpSessionEnvelope.metadata(), null)))
                            .build();
            resultPrompts.add(reasonWhyNotAttached);

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                            .build());
        }

        return judicialResults;
    }


    private List<JudicialResult> collectionOrder(final FinancialImposition financialImposition,
                                                 final JsonEnvelope sjpSessionEnvelope,
                                                 final ZonedDateTime resultedOn) {
        final UUID resultId = COLLO.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();
        final Payment payment = financialImposition.getPayment();
        final CostsAndSurcharge costsAndSurcharge = financialImposition.getCostsAndSurcharge();

        if (nonNull(costsAndSurcharge)
                && costsAndSurcharge.isCollectionOrderMade()) {
            final JudicialResultPrompt collectionOrderType =
                    getPrompt(COLLECTION_ORDER_TYPE, resultDefinition)
                            .withJudicialResultPromptTypeId(COLLECTION_ORDER_TYPE.getId())
                            .withValue(jCachedReferenceData.getPaymentType(paymentTypeMap.get(payment.getPaymentType().toString()),
                                    envelopeFrom(sjpSessionEnvelope.metadata(), null)))
                            .build();
            resultPrompts.add(collectionOrderType);

            ofNullable(payment.getReasonWhyNotAttachedOrDeducted())
                    .ifPresent(e ->
                            resultPrompts.add(getPrompt(REASON_NOT_ABD_OR_AEO, resultDefinition)
                                    .withJudicialResultPromptTypeId(REASON_NOT_ABD_OR_AEO.getId())
                                    .withValue(payment.getReasonWhyNotAttachedOrDeducted())
                                    .build()));

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                            .build());

        }
        return judicialResults;
    }


    private List<JudicialResult> attachToEarnings(final Payment payment,
                                                  final JsonEnvelope sjpSessionEnvelope,
                                                  final UUID defendantId,
                                                  final ZonedDateTime resultedOn) {
        final UUID resultId = AEOC.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();
        final PaymentType paymentType = payment.getPaymentType();


        // employer
        final EmployerDetails employerDetails = sjpService.getEmployerDetails(defendantId, envelopeFrom(sjpSessionEnvelope.metadata(), null));
        if (PaymentType.ATTACH_TO_EARNINGS.equals(paymentType)) {

            // reason for deducting from benefits
            addJudicialResultPrompt(AEOC_REASON,
                    ofNullable(jCachedReferenceData
                            .getDeductFromFundsReason(deductFromBenefitsMap
                                            .get(payment.getReasonForDeductingFromBenefits().name()),
                                    envelopeFrom(sjpSessionEnvelope.metadata(), null))),
                    resultPrompts, resultDefinition);

            // employer
            final Optional<Address> addressOptional = ofNullable(employerDetails).map(EmployerDetails::getAddress);
            addJudicialResultPrompt(EMPLOYER_NAME, ofNullable(employerDetails).map(EmployerDetails::getName), resultPrompts, resultDefinition);
            addJudicialResultPrompt(EMPLOYEE_REFERENCE_NO, ofNullable(employerDetails).map(EmployerDetails::getEmployeeReference), resultPrompts, resultDefinition);
            addJudicialResultPrompt(EMPLOYER_ADDRESS_LINE_1, addressOptional.map(Address::getAddress1), resultPrompts, resultDefinition);
            addJudicialResultPrompt(EMPLOYER_ADDRESS_LINE_2, addressOptional.map(Address::getAddress2), resultPrompts, resultDefinition);
            addJudicialResultPrompt(EMPLOYER_ADDRESS_LINE_3, addressOptional.map(Address::getAddress3), resultPrompts, resultDefinition);
            addJudicialResultPrompt(EMPLOYER_ADDRESS_LINE_4, addressOptional.map(Address::getAddress4), resultPrompts, resultDefinition);
            addJudicialResultPrompt(EMPLOYER_ADDRESS_LINE_5, addressOptional.map(Address::getAddress5), resultPrompts, resultDefinition);
            addJudicialResultPrompt(EMPLOYER_POSTCODE, addressOptional.map(Address::getPostcode), resultPrompts, resultDefinition);

            // judicialResults
            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                            .build());
        }

        return judicialResults;
    }

    private List<JudicialResult> victimSurcharge(final BigDecimal victimSurchargeValue,
                                                 final JsonEnvelope sjpSessionEnvelope,
                                                 final ZonedDateTime resultedOn) {
        final UUID resultId = FVS.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();

        addJudicialResultPrompt(AMOUNT_OF_SURCHARGE, ofNullable(getCurrencyAmount(victimSurchargeValue.toString())), resultPrompts, resultDefinition);

        // judicialResults
        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                        .build());

        return judicialResults;
    }

    private List<JudicialResult> noVictimSurcharge(final JsonEnvelope sjpSessionEnvelope,
                                                   final ZonedDateTime resultedOn,
                                                   final String reasonVictimSurcharge) {
        final UUID resultId = NOVS.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();

        addJudicialResultPrompt(REASON_FOR_NOT_IMPOSING_OR_REDUCING_VICTIM_SURCHARGE, ofNullable(reasonVictimSurcharge), resultPrompts, resultDefinition);

        // judicialResults
        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                        .build());

        return judicialResults;
    }

    private List<JudicialResult> paymentTerms(final PaymentTerms paymentTerms,
                                              final JsonEnvelope sjpSessionEnvelope,
                                              final ZonedDateTime resultedOn) {

        final boolean isReserveTerms = paymentTerms.isReserveTerms();

        final LumpSum lumpSum = paymentTerms.getLumpSum();
        final Installments installments = paymentTerms.getInstallments();

        if (nonNull(lumpSum) && nonNull(installments)) {
            return lumpSumAndInstallments(isReserveTerms, lumpSum, installments, sjpSessionEnvelope, resultedOn);
        } else if (nonNull(lumpSum)) {
            return lumpSum(isReserveTerms, lumpSum, sjpSessionEnvelope, resultedOn);
        } else if (nonNull(installments)) {
            return installments(isReserveTerms, installments, sjpSessionEnvelope, resultedOn);
        }

        return new ArrayList<>();
    }

    private List<JudicialResult> lumpSum(final boolean isReserveTerms,
                                         final LumpSum lumpSum,
                                         final JsonEnvelope sjpSessionEnvelope,
                                         final ZonedDateTime resultedOn) {
        final UUID resultId = isReserveTerms ?
                RLSUM.getResultDefinitionId() : LSUM.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();

        if (resultId.equals(LSUM.getResultDefinitionId())) {
            getLumSumWithInDate(lumpSum.getWithinDays(), LocalDate.of(resultedOn.getYear(), resultedOn.getMonth(), resultedOn.getDayOfMonth()))
                    .ifPresent(e -> addJudicialResultPrompt(LSUM_DATE, ofNullable(e), resultPrompts, resultDefinition));
        } else {
            ofNullable(lumpSum.getWithinDays()).ifPresent(e ->
                    addJudicialResultPrompt(PAY_WITHIN_DAYS, ofNullable(format("lump sum %d days", e)), resultPrompts, resultDefinition));
        }

        // judicialResults
        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                        .build());

        return judicialResults;
    }

    private List<JudicialResult> lumpSumAndInstallments(final boolean isReserveTerms,
                                                        final LumpSum lumpSum,
                                                        final Installments installments,
                                                        final JsonEnvelope sjpSessionEnvelope,
                                                        final ZonedDateTime resultedOn) {
        final UUID resultId = isReserveTerms ? RLSUMI.getResultDefinitionId() : LUMSI.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        if (resultId.equals(RLSUMI.getResultDefinitionId())) {
            ofNullable(lumpSum.getAmount()).ifPresent(e ->
                    addJudicialResultPrompt(RLSUMI_LUMP_SUM_AMOUNT, ofNullable(getCurrencyAmount(e.toString())), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getAmount()).ifPresent(e ->
                    addJudicialResultPrompt(RLSUMI_INSTALMENT_AMOUNT, ofNullable(getCurrencyAmount(e.toString())), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getStartDate()).ifPresent(e ->
                    addJudicialResultPrompt(RLSUMI_INSTALMENT_START_DATE, ofNullable(e.toString()), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getPeriod()).ifPresent(e ->
                    addJudicialResultPrompt(RLSUMI_PAYMENT_FREQUENCY, ofNullable(
                            jCachedReferenceData.getInstallmentFrequency(installmentPeriodMap.get(e.name()),
                                    envelopeFrom(sjpSessionEnvelope.metadata(), null))
                    ), judicialResultPrompts, resultDefinition));
        } else if (resultId.equals(LUMSI.getResultDefinitionId())) {
            ofNullable(lumpSum.getAmount()).ifPresent(e ->
                    addJudicialResultPrompt(LUMSI_LUMP_SUM_AMOUNT, ofNullable(getCurrencyAmount(e.toString())), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getAmount()).ifPresent(e ->
                    addJudicialResultPrompt(LUMSI_INSTALMENT_AMOUNT, ofNullable(getCurrencyAmount(e.toString())), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getStartDate()).ifPresent(e ->
                    addJudicialResultPrompt(LUMSI_INSTALMENT_START_DATE, ofNullable(e.toString()), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getPeriod()).ifPresent(e ->
                    addJudicialResultPrompt(LUMSI_PAYMENT_FREQUENCY, ofNullable(
                            jCachedReferenceData.getInstallmentFrequency(installmentPeriodMap.get(e.name()),
                                    envelopeFrom(sjpSessionEnvelope.metadata(), null))), judicialResultPrompts, resultDefinition));
        }

        // judicialResults
        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                        .build());

        return judicialResults;
    }

    private List<JudicialResult> installments(final boolean isReserveTerms,
                                              final Installments installments,
                                              final JsonEnvelope sjpSessionEnvelope,
                                              final ZonedDateTime resultedOn) {
        final UUID resultId = isReserveTerms ?
                RINSTL.getResultDefinitionId() : INSTL.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        if (resultId.equals(RINSTL.getResultDefinitionId())) {
            ofNullable(installments.getAmount()).ifPresent(e ->
                    addJudicialResultPrompt(RINSTL_INSTALMENT_AMOUNT, ofNullable(getCurrencyAmount(e.toString())), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getStartDate()).ifPresent(e ->
                    addJudicialResultPrompt(RINSTL_INSTALMENT_START_DATE, ofNullable(e.toString()), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getPeriod()).ifPresent(e ->
                    addJudicialResultPrompt(RINSTL_PAYMENT_FREQUENCY, ofNullable(
                            jCachedReferenceData.getInstallmentFrequency(installmentPeriodMap.get(e.name()),
                                    envelopeFrom(sjpSessionEnvelope.metadata(), null))), judicialResultPrompts, resultDefinition));
        } else if (resultId.equals(INSTL.getResultDefinitionId())) {
            ofNullable(installments.getAmount()).ifPresent(e ->
                    addJudicialResultPrompt(INSTALMENTS_AMOUNT, ofNullable(getCurrencyAmount(e.toString())), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getStartDate()).ifPresent(e ->
                    addJudicialResultPrompt(INSTL_INSTALMENT_START_DATE, ofNullable(e.toString()), judicialResultPrompts, resultDefinition));
            ofNullable(installments.getPeriod()).ifPresent(e ->
                    addJudicialResultPrompt(INSTL_PAYMENT_FREQUENCY, ofNullable(
                            jCachedReferenceData.getInstallmentFrequency(installmentPeriodMap.get(e.name()),
                                    envelopeFrom(sjpSessionEnvelope.metadata(), null))), judicialResultPrompts, resultDefinition));
        }

        // judicialResults
        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                        .build());

        return judicialResults;
    }

    private void addJudicialResultPrompt(final JPrompt jPrompt,
                                         final Optional<String> valueOptional,
                                         final List<JudicialResultPrompt> resultPrompts,
                                         final JsonObject resultDefinition) {
        final JudicialResultPrompt.Builder judicialResultPromptBuilder = getPrompt(jPrompt, resultDefinition)
                .withJudicialResultPromptTypeId(jPrompt.getId());

        valueOptional.ifPresent(value ->
                resultPrompts.add(judicialResultPromptBuilder
                        .withValue(value)
                        .build()));
    }

    private Optional<String> getLumSumWithInDate(final Integer value,
                                                 final LocalDate resultedOn) {
        if (Objects.nonNull(value) && value.equals(FOURTEEN_DAYS)) {
            return of(LocalDates.to(resultedOn.plusDays(FOURTEEN_DAYS)));
        } else if (Objects.nonNull(value) && value.equals(TWENTY_EIGHT_DAYS)) {
            return of(LocalDates.to(resultedOn.plusDays(TWENTY_EIGHT_DAYS)));
        }
        return Optional.empty();
    }

    private String getNoVictimSurchargeReason(final DecisionSaved decisionSaved) {
        final CostsAndSurcharge costsAndSurcharge =
                decisionSaved.getFinancialImposition().getCostsAndSurcharge();
        if (allAbsoluteDischarge(decisionSaved)) {
            return "Absolute Discharge";
        } else if (costsAndSurcharge != null) {
            return costsAndSurcharge.getReasonForNoCosts();
        }
        return null;
    }

    private boolean allAbsoluteDischarge(final DecisionSaved decisionSaved) {
        return decisionSaved
                .getOffenceDecisions()
                .stream()
                .allMatch(a -> (a instanceof Discharge && ((Discharge) a).getDischargeType().equals(ABSOLUTE)));
    }


}
