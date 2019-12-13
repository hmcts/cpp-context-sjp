package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DecisionName.ADJOURN;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ABSOLUTE_DISCHARGE_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.AMOUNT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.APPLICATION_MADE_FOR_BENEFIT_DEDUCTIONS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ATTACHMENT_OF_EARNINGS_ORDER;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ATTACH_TO_EARNINGS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.COLLECTION_ORDER_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.COLLECTION_ORDER_MADE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.COMPENSATION;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.COMPENSATION_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.COMPENSATION_ORDERED;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.CONDITIONAL_DISCHARGE_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.COSTS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.COSTS_AND_SURCHARGE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DEDUCT_FROM_BENEFITS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DEFENDANT_KNOWN_DEFAULTER;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DEFENDANT_REQUESTED;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DISCHARGE_FOR;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DISCHARGE_FOR_DAY;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DISCHARGE_FOR_MONTH;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DISCHARGE_FOR_WEEK;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DISCHARGE_FOR_YEAR;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DISCHARGE_TYPE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DISMISS_RESULT_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FINANCIAL_COSTS_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FINANCIAL_IMPOSITION;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FINE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FINE_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.HOUR_FORMAT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.INDEX;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.INSTALLMENTS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.INSTALLMENTS_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LUMP_SUM;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LUMP_SUM_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LUMP_SUM_PLUS_INSTALLMENTS_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.MAGISTRATES_COURT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.NO_COMPENSATION_REASON;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.NO_COMPENSATION_REASON_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.NO_VICTIM_SURCHARGE_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.OFFENCE_DECISIONS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.OFFENCE_DECISION_INFORMATION;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PAYMENT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PAYMENT_TERMS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PAYMENT_TYPE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PERIOD;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PROVED_SJP_NAME;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REASON;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REASON_FOR_DEDUCTING_FROM_BENEFITS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REASON_WHY_NOT_ATTACHED_OR_DEDUCTED;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REFERRED_FOR_FUTURE_SJP_SESSION_RESULT_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REFERRED_TO_COURT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REFERRED_TO_DATE_TIME;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REFERRED_TO_OPEN_COURT_RESULT_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.REFERRED_TO_ROOM;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.RESERVE_TERMS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.RESERVE_TERMS_INSTALLMENTS_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.RESERVE_TERMS_LUMP_SUM_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.RESERVE_TERMS_LUMP_SUM_PLUS_INSTALLMENTS_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.RESULTS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.RESULT_TYPE_ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.START_DATE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TERMINAL_ENTRIES;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TERMINAL_ENTRIES_DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TOTAL_SUM;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TYPE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.UNIT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.VALUE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.VERDICT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.VICTIM_SURCHARGE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.VICTIM_SURCHARGE_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.WITHDRAW_REASON_ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.WITHDRAW_RESULT_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.WITHIN_DAYS;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

public class ReferencedDecisionSavedOffenceConverter {

    private static final Map<String, String> dischargeForMap = ImmutableMap.of(
            DISCHARGE_FOR_DAY, "Day(s)",
            DISCHARGE_FOR_WEEK, "Week(s)",
            DISCHARGE_FOR_MONTH, "Month(s)",
            DISCHARGE_FOR_YEAR, "Year(s)"
    );

    private static final Map<String, String> paymentTypeMap = ImmutableMap.of(
            PAY_TO_COURT, "Pay directly to court",
            DEDUCT_FROM_BENEFITS, "Deduct from benefits",
            ATTACH_TO_EARNINGS,"Attach to earnings"
    );

    private static final Map<String,String> deductFromBenefitsMap = ImmutableMap.of(
            COMPENSATION_ORDERED,"Compensation ordered",
            DEFENDANT_KNOWN_DEFAULTER,"Defendant known defaulter",
            DEFENDANT_REQUESTED,"Defendant requested"
    );

    @Inject
    private ReferenceDataService referenceDataService;

    public JsonArray convertOffenceDecisions(final JsonEnvelope decisionSavedEvent) {
        final JsonObject decisionSavedEventPayload = decisionSavedEvent.payloadAsJsonObject();
        final JsonArray offences = decisionSavedEventPayload.getJsonArray(OFFENCE_DECISIONS);
        final JsonObject financialImposition = decisionSavedEventPayload.getJsonObject(FINANCIAL_IMPOSITION);

        final CachedReferenceData referenceData = getCachedReferenceData(decisionSavedEvent);

        final JsonArrayBuilder decisionArray = offences.getValuesAs(JsonObject.class)
                .stream()
                .flatMap(offenceDecision -> convertOffenceDecision(offenceDecision, referenceData).stream())
                .reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add);

        if (nonNull(financialImposition)) {
            decisionArray.add(convertFinancialImposition(financialImposition, referenceData));
        }

        return decisionArray.build();
    }

    @VisibleForTesting
    CachedReferenceData getCachedReferenceData(final JsonEnvelope envelope) {
        return new CachedReferenceData(referenceDataService, envelope);
    }

    private List<JsonObject> convertOffenceDecision(final JsonObject offenceDecision, final CachedReferenceData referenceData) {
        final DecisionType decisionType = DecisionType.valueOf(offenceDecision.getString(TYPE));

        switch (decisionType) {
            case WITHDRAW:
                return convertWithdrawDecision(offenceDecision, referenceData);
            case DISMISS:
                return convertDismissDecision(offenceDecision, referenceData);
            case ADJOURN:
                return convertAdjournDecision(offenceDecision, referenceData);
            case DISCHARGE:
                return convertDischargeDecision(offenceDecision, referenceData);
            case FINANCIAL_PENALTY:
                return convertFinancialPenaltyDecision(offenceDecision, referenceData);
            case REFERRED_TO_OPEN_COURT:
                return convertReferredToOpenCourtDecision(offenceDecision, referenceData);
            case REFERRED_FOR_FUTURE_SJP_SESSION:
                return convertReferredForFutureSjpDecision(offenceDecision, referenceData);
            default:
                return emptyList();
        }
    }

    private JsonObject convertFinancialImposition(JsonObject financialImposition, CachedReferenceData referenceData) {
        final JsonArrayBuilder resultsArray = createArrayBuilder();
        final JsonObject costsAndSurcharge = financialImposition.getJsonObject(COSTS_AND_SURCHARGE);
        final JsonObject payment = financialImposition.getJsonObject(PAYMENT);
        final JsonObject paymentTerms =  payment.getJsonObject(PAYMENT_TERMS);

        resultsArray.add(costsResult(costsAndSurcharge, referenceData));
        if (collectionOrderMade(costsAndSurcharge)) {
            resultsArray.add(collectionOrder(payment, referenceData));
        }

        final JsonNumber victimSurchargeValue = costsAndSurcharge.getJsonNumber(VICTIM_SURCHARGE);
        if(victimSurchargeValue.doubleValue() > 0){
            resultsArray.add(victimSurcharge(victimSurchargeValue, referenceData));
        } else {
            resultsArray.add(noVictimSurcharge(referenceData));
        }

        final String paymentType = payment.getString(PAYMENT_TYPE);

        if(paymentType.equals(DEDUCT_FROM_BENEFITS)){
            resultsArray.add(deductFromBenefits(payment, referenceData));
        } else if(paymentType.equals(ATTACH_TO_EARNINGS)){
            resultsArray.add(attachToEarnings(payment, referenceData));
        }

        resultsArray.add(paymentTerms(paymentTerms, referenceData));

        return createObjectBuilder().add(RESULTS, resultsArray).build();
    }

    private List<JsonObject> convertWithdrawDecision(final JsonObject offenceDecision, final CachedReferenceData referenceData) {
        final UUID withdrawalReasonId = UUID.fromString(offenceDecision.getString(WITHDRAW_REASON_ID));
        final String withdrawalReason = referenceData.getWithdrawalReason(withdrawalReasonId);
        final JsonObject withdrawalReasonTerminalEntry = terminalEntry(2, withdrawalReason);
        final JsonObject offenceDecisionInformation = offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getJsonObject(0);

        final JsonObject convertedWithdrawDecision = convertDecision(offenceDecisionInformation, WITHDRAW_RESULT_CODE, withdrawalReasonTerminalEntry, referenceData);

        return asList(convertedWithdrawDecision);
    }

    private List<JsonObject> convertReferredToOpenCourtDecision(JsonObject offenceDecision, CachedReferenceData referenceData) {
        final JsonArrayBuilder referredToOpenCourtEntryTerminalEntries = createArrayBuilder()
                .add(terminalEntry(5, ZonedDateTime.parse(offenceDecision.getString(REFERRED_TO_DATE_TIME)).format(DATE_FORMAT)))
                .add(terminalEntry(10, ZonedDateTime.parse(offenceDecision.getString(REFERRED_TO_DATE_TIME)).format(HOUR_FORMAT)))
                .add(terminalEntry(15, offenceDecision.getString(REFERRED_TO_COURT)))
                .add(terminalEntry(20, offenceDecision.getString(MAGISTRATES_COURT)))
                .add(terminalEntry(30, offenceDecision.getJsonNumber(REFERRED_TO_ROOM).toString() ))
                .add(terminalEntry(35, offenceDecision.getString(REASON)));

        return offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getValuesAs(JsonObject.class).stream()
                .map(offenceDecisionInformation -> createObjectBuilder()
                        .add(ID, offenceDecisionInformation.getString(OFFENCE_ID))
                        .add(VERDICT, offenceDecisionInformation.getString(VERDICT))
                        .add(RESULTS, createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add(CODE, REFERRED_TO_OPEN_COURT_RESULT_CODE)
                                        .add(RESULT_TYPE_ID, referenceData.getResultId(REFERRED_TO_OPEN_COURT_RESULT_CODE).toString())
                                        .add(TERMINAL_ENTRIES, referredToOpenCourtEntryTerminalEntries)))
                        .build())
                .collect(toList());
    }

    private List<JsonObject> convertReferredForFutureSjpDecision(JsonObject offenceDecision, CachedReferenceData referenceData) {
        return offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getValuesAs(JsonObject.class).stream()
                .map(offenceDecisionInformation -> convertDecision(offenceDecisionInformation,
                        REFERRED_FOR_FUTURE_SJP_SESSION_RESULT_CODE,
                        null,
                        referenceData))
                .collect(toList());
    }

    private List<JsonObject> convertDismissDecision(final JsonObject offenceDecision, final CachedReferenceData referenceData) {
        final JsonObject convertedDismissDecision = convertDecision(offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getJsonObject(0),
                DISMISS_RESULT_CODE,
                null,
                referenceData);

        return asList(convertedDismissDecision);
    }

    private List<JsonObject> convertAdjournDecision(final JsonObject offenceDecision, final CachedReferenceData referenceData) {
        return offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getValuesAs(JsonObject.class).stream()
                .map(offenceDecisionInformation -> convertDecision(offenceDecisionInformation, ADJOURN, terminalEntry(-1, offenceDecision.getString("adjournTo")), referenceData) )
                .collect(toList());
    }

    private JsonObject convertDecision(JsonObject offenceDecisionInformation, final String decisionType, final JsonObject terminalEntry, final CachedReferenceData referenceData) {
        final JsonObjectBuilder result = createObjectBuilder()
                .add(CODE, decisionType)
                .add(RESULT_TYPE_ID, referenceData.getResultId(decisionType).toString());

        if(nonNull(terminalEntry)) {
                result.add(TERMINAL_ENTRIES, terminalEntries(terminalEntry));
        } else {
            result.add(TERMINAL_ENTRIES, createArrayBuilder());
        }

        return createObjectBuilder()
                .add(ID, offenceDecisionInformation.getString(OFFENCE_ID))
                .add(VERDICT, offenceDecisionInformation.getString(VERDICT))
                .add(RESULTS, createArrayBuilder().add(result))
                .build();
    }

    private List<JsonObject> convertDischargeDecision(final JsonObject offenceDecision, CachedReferenceData referenceData) {
        final JsonArrayBuilder results = createArrayBuilder().add(dischargeResult(offenceDecision, referenceData));
        return convertFinancialOffence(offenceDecision, results, referenceData);
    }

    private List<JsonObject> convertFinancialOffence(JsonObject offenceDecision, JsonArrayBuilder results, CachedReferenceData referenceData) {
        if (offenceDecision.containsKey(COMPENSATION)) {
            results.add(compensationResult(offenceDecision, referenceData));
        }

        if (offenceDecision.containsKey(NO_COMPENSATION_REASON)) {
            results.add(noCompensationReasonResult(offenceDecision, referenceData));
        }

        final JsonObject offenceDecisionInformation = offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getJsonObject(0);

        return asList(createOffence(offenceDecisionInformation, results));
    }

    private List<JsonObject> convertFinancialPenaltyDecision(final JsonObject offenceDecision, CachedReferenceData referenceData) {
        final JsonArrayBuilder results = createArrayBuilder().add(fineResult(offenceDecision, referenceData));
        return convertFinancialOffence(offenceDecision, results, referenceData);
    }

    private JsonObject costsResult(final JsonObject sjpCostsAndSurcharge, CachedReferenceData referenceData) {
        return createObjectBuilder()
                .add(CODE, FINANCIAL_COSTS_CODE)
                .add(RESULT_TYPE_ID, referenceData.getResultId(FINANCIAL_COSTS_CODE).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(1, sjpCostsAndSurcharge.getJsonNumber(COSTS).toString()))
                )
                .build();
    }

    private JsonObject collectionOrder(final JsonObject sjpPayment, CachedReferenceData referenceData) {
        final JsonArrayBuilder terminalEntries = createArrayBuilder()
                .add(terminalEntry(4, paymentTypeMap.get(sjpPayment.getString(PAYMENT_TYPE))));

        terminalEntries.add(terminalEntry(9, sjpPayment.containsKey(REASON_WHY_NOT_ATTACHED_OR_DEDUCTED) ?
                sjpPayment.getString(REASON_WHY_NOT_ATTACHED_OR_DEDUCTED): "")
        );

        final JsonObjectBuilder collectionOrder = createObjectBuilder()
                .add(CODE, COLLECTION_ORDER_CODE)
                .add(RESULT_TYPE_ID, referenceData.getResultId(COLLECTION_ORDER_CODE).toString())
                .add(TERMINAL_ENTRIES, terminalEntries);

        return collectionOrder.build();
    }

    private boolean collectionOrderMade(JsonObject costsAndSurcharge) {
        return costsAndSurcharge.containsKey(COLLECTION_ORDER_MADE) && costsAndSurcharge.getBoolean(COLLECTION_ORDER_MADE);
    }

    private JsonObject createOffence(final JsonObject offenceDecisionInformation, final JsonArrayBuilder  results) {
        return createObjectBuilder()
                .add(ID, offenceDecisionInformation.getString(OFFENCE_ID))
                .add(PROVED_SJP_NAME, PROVED_SJP.equals(offenceDecisionInformation.getString(VERDICT)))
                .add(VERDICT, offenceDecisionInformation.getString(VERDICT))
                .add(RESULTS, results).
                        build();
    }

    private JsonObjectBuilder compensationResult(JsonObject offenceDecision, CachedReferenceData referenceData) {
        return createObjectBuilder()
                .add(CODE, COMPENSATION_CODE)
                .add(RESULT_TYPE_ID, referenceData.getResultId(COMPENSATION_CODE).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder().
                        add(terminalEntry(1, offenceDecision.getJsonNumber(COMPENSATION).toString())));
    }

    private JsonObjectBuilder noCompensationReasonResult(JsonObject offenceDecision, CachedReferenceData referenceData) {
        return createObjectBuilder()
                .add(CODE, NO_COMPENSATION_REASON_CODE)
                .add(RESULT_TYPE_ID, referenceData.getResultId(NO_COMPENSATION_REASON_CODE).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder().
                        add(terminalEntry(1, offenceDecision.getString(NO_COMPENSATION_REASON))));
    }

    private JsonObjectBuilder dischargeResult(JsonObject offenceDecision, CachedReferenceData referenceData) {
        final String dischargeType = offenceDecision.getString(DISCHARGE_TYPE);
        final String code = ABSOLUTE.equals(dischargeType) ? ABSOLUTE_DISCHARGE_CODE : CONDITIONAL_DISCHARGE_CODE;

        final JsonArrayBuilder dischargeEntryTerminalEntries = createArrayBuilder();
        if (CONDITIONAL.equals(dischargeType) && offenceDecision.containsKey(DISCHARGE_FOR)) {
            final JsonObject dischargedFor = offenceDecision.getJsonObject(DISCHARGE_FOR);
            dischargeEntryTerminalEntries.add(terminalEntry(1, dischargedFor.getJsonNumber(VALUE).toString()));
            dischargeEntryTerminalEntries.add(terminalEntry(2, dischargeForMap.get(dischargedFor.getString(UNIT))));
        }

        return createObjectBuilder()
                .add(CODE, code)
                .add(RESULT_TYPE_ID, referenceData.getResultId(code).toString())
                .add(TERMINAL_ENTRIES, dischargeEntryTerminalEntries);
    }

    private JsonObjectBuilder fineResult(JsonObject offenceDecision, CachedReferenceData referenceData) {
        final JsonArrayBuilder terminalEntries = createArrayBuilder();

        if(offenceDecision.containsKey(FINE)){
            terminalEntries.add(terminalEntry(1, offenceDecision.getJsonNumber(FINE).toString()));
        }

        return createObjectBuilder()
                .add(CODE, FINE_CODE)
                .add(RESULT_TYPE_ID, referenceData.getResultId(FINE_CODE).toString())
                .add(TERMINAL_ENTRIES, terminalEntries);
    }

    private JsonArray terminalEntries(final JsonObject... terminalEntries) {
        if(nonNull(terminalEntries)) {
            return Arrays.stream(terminalEntries).reduce(Json.createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add).build();
        }
        return createArrayBuilder().build();
    }

    private JsonObject victimSurcharge(final JsonNumber victimSurchargeValue, CachedReferenceData referenceData){
        return createObjectBuilder()
                .add(CODE, VICTIM_SURCHARGE_CODE)
                .add(RESULT_TYPE_ID, referenceData.getResultId(VICTIM_SURCHARGE_CODE).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(1, victimSurchargeValue.toString()))
                ).build();
    }

    private JsonObject noVictimSurcharge(CachedReferenceData referenceData){
        return createObjectBuilder()
                .add(CODE, NO_VICTIM_SURCHARGE_CODE)
                .add(RESULT_TYPE_ID, referenceData.getResultId(NO_VICTIM_SURCHARGE_CODE).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(5, "not imposed"))
                        .add(terminalEntry(10, "Absolute Discharge"))
                ).build();
    }

    private JsonObject deductFromBenefits(final JsonObject sjpPayment, CachedReferenceData referenceData){
        return createObjectBuilder()
                .add(CODE, APPLICATION_MADE_FOR_BENEFIT_DEDUCTIONS)
                .add(RESULT_TYPE_ID, referenceData.getResultId(APPLICATION_MADE_FOR_BENEFIT_DEDUCTIONS).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(1, sjpPayment.getJsonNumber(TOTAL_SUM).toString()))
                        .add(terminalEntry(2, deductFromBenefitsMap.get(sjpPayment.getString(REASON_FOR_DEDUCTING_FROM_BENEFITS))))
                ).build();
    }

    private JsonObject attachToEarnings(final JsonObject payment, CachedReferenceData referenceData){
        return createObjectBuilder()
                .add(CODE, ATTACHMENT_OF_EARNINGS_ORDER)
                .add(RESULT_TYPE_ID, referenceData.getResultId(ATTACHMENT_OF_EARNINGS_ORDER).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(15, payment.getJsonNumber(TOTAL_SUM).toString()))
                        .add(terminalEntry(10, deductFromBenefitsMap.get(payment.getString(REASON_FOR_DEDUCTING_FROM_BENEFITS))))
                ).build();
    }

    private JsonObject paymentTerms(final JsonObject sjpPaymentTerms, CachedReferenceData referenceData) {
        final boolean lumpSumSelected = sjpPaymentTerms.containsKey(LUMP_SUM);
        final boolean installmentsSelected = sjpPaymentTerms.containsKey(INSTALLMENTS);
        final boolean isReserveTerms = sjpPaymentTerms.getBoolean(RESERVE_TERMS);
        final String lumpSumAndInstallmentsCode = isReserveTerms ?
                RESERVE_TERMS_LUMP_SUM_PLUS_INSTALLMENTS_CODE : LUMP_SUM_PLUS_INSTALLMENTS_CODE;
        final String lumpSumCode = isReserveTerms ? RESERVE_TERMS_LUMP_SUM_CODE : LUMP_SUM_CODE;
        final String installmentsCode = isReserveTerms ? RESERVE_TERMS_INSTALLMENTS_CODE : INSTALLMENTS_CODE;
        if(lumpSumSelected && installmentsSelected){
            return lumpSumAndInstallments(
                    lumpSumAndInstallmentsCode,
                    sjpPaymentTerms.getJsonObject(LUMP_SUM),
                    sjpPaymentTerms.getJsonObject(INSTALLMENTS),
                    referenceData);
        } else if(lumpSumSelected) {
            return lumpSum(
                    lumpSumCode,
                    sjpPaymentTerms.getJsonObject(LUMP_SUM),
                    referenceData);
        } else if(installmentsSelected) {
            return installments(installmentsCode, sjpPaymentTerms.getJsonObject(INSTALLMENTS), referenceData);
        } else {
            throw new IllegalArgumentException("invalid payment terms doesn't contain installments or lump sum");
        }
    }

    private JsonObject terminalEntry(final int index, final String value) {
        return createObjectBuilder()
                .add(INDEX, index)
                .add(VALUE, value)
                .build();
    }

    private JsonObject lumpSum(final String code, final JsonObject sjpLumpSum, CachedReferenceData referenceData){
        return createObjectBuilder()
                .add(CODE, code)
                .add(RESULT_TYPE_ID, referenceData.getResultId(code).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(5, sjpLumpSum.getJsonNumber(AMOUNT).toString()))
                        .add(terminalEntry(6, String.format("Lump sum within %d days", sjpLumpSum.getInt(WITHIN_DAYS))))
                ).build();
    }

    private JsonObject lumpSumAndInstallments(final String code, final JsonObject sjpLumpSum, final JsonObject sjpInstallments, CachedReferenceData referenceData) {
        return createObjectBuilder()
                .add(CODE, code)
                .add(RESULT_TYPE_ID, referenceData.getResultId(code).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(96, sjpLumpSum.getJsonNumber(AMOUNT).toString()))
                        .add(terminalEntry(98, sjpInstallments.getJsonNumber(AMOUNT).toString()))
                        .add(terminalEntry(99, asTerminalEntryDate(sjpInstallments.getString(START_DATE))))
                        .add(terminalEntry(100, sjpInstallments.getString(PERIOD).toLowerCase()))
                ).build();
    }

    private JsonObject installments(final String code, final JsonObject sjpInstallments, CachedReferenceData referenceData) {
        return createObjectBuilder()
                .add(CODE, code)
                .add(RESULT_TYPE_ID, referenceData.getResultId(code).toString())
                .add(TERMINAL_ENTRIES, createArrayBuilder()
                        .add(terminalEntry(96, sjpInstallments.getJsonNumber(AMOUNT).toString()))
                        .add(terminalEntry(97, sjpInstallments.getString(PERIOD).toLowerCase()))
                        .add(terminalEntry(98, asTerminalEntryDate(sjpInstallments.getString(START_DATE))))
                ).build();
    }

    private String asTerminalEntryDate(String startDate) {
        return LocalDate.parse(startDate).format(TERMINAL_ENTRIES_DATE_FORMAT);
    }
}
