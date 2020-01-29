package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class TransformationConstants {


    private TransformationConstants(){
    }

    public static final String CONDITIONAL = "CONDITIONAL";
    public static final String ABSOLUTE = "ABSOLUTE";
    public static final String ID = "id";
    public static final String TYPE = "type";

    public static final Map<String, String> verdictCodeMap = ImmutableMap.of(
            "NO_VERDICT", "NO_VERDICT",
            "PSJ", "PROVED_SJP",
            "FNG", "FOUND_NOT_GUILTY",
            "GSJ", "FOUND_GUILTY"
    );

    public static final Map<String, String> dischargeForUnitMap = ImmutableMap.of(
            "Day(s)", "DAY",
            "Week(s)", "WEEK",
            "Month(s)", "MONTH",
            "Year(s)", "YEAR"
    );

    public static final Map<String, String> paymentTypeMap = ImmutableMap.of(
            "Pay directly to court", "PAY_TO_COURT",
            "Deduct from benefits", "DEDUCT_FROM_BENEFITS",
            "Attach to earnings", "ATTACH_TO_EARNINGS"
    );

    public static final Map<String, String> deductFromBenefitsMap = ImmutableMap.of(
            "Compensation ordered", "COMPENSATION_ORDERED",
            "Defendant known defaulter", "DEFENDANT_KNOWN_DEFAULTER",
            "Defendant is a known defaulter", "DEFENDANT_KNOWN_DEFAULTER",
            "Defendant requested", "DEFENDANT_REQUESTED",
            "Defendant requested this", "DEFENDANT_REQUESTED"
    );

    public static final Map<String, Integer> lumpSumWithInDaysMap = ImmutableMap.of(
            "Lump sum within 14 days", 14,
            "Lump sum within 28 days", 28
    );


    // new result type
    public static final String DISCHARGE = "DISCHARGE";

    // event attributes
    public static final String OFFENCE_ID = "offenceId";
    public static final String VERDICT = "verdict";

    public static final String WITHDRAWAL_REASON_ID = "withdrawalReasonId";

    // discharge and financial penalty
    public static final String DISCHARGE_TYPE = "dischargeType";
    public static final String COMPENSATION = "compensation";
    public static final String NO_COMPENSATION_REASON = "noCompensationReason";
    public static final String GUILTY_PLEA_TAKEN_INTO_ACCOUNT = "guiltyPleaTakenIntoAccount";
    public static final String DISCHARGED_FOR = "dischargedFor";
    public static final String OFFENCE_DECISION_INFORMATION = "offenceDecisionInformation";


    // financial imposition
    public static final String FINE = "fine";
    public static final String TOTAL_SUM = "totalSum";
    public static final String REASON_FOR_DEDUCTING_FROM_BENEFITS = "reasonForDeductingFromBenefits";
    public static final String REASON_FOR_ATTACHING_TO_EARNINGS = "reasonForAttachingToEarnings";
    public static final String COSTS_AND_SURCHARGE = "costsAndSurcharge";
    public static final String PAYMENT = "payment";

    public static final String REASON_WHY_NOT_ATTACHED_OR_DEDUCTED = "reasonWhyNotAttachedOrDeducted";
    public static final String PAYMENT_TYPE = "paymentType";
    public static final String COLLECTION_ORDER_MADE = "collectionOrderMade";
    public static final String VICTIM_SURCHARGE_REASON = "victimSurchargeReason";
    public static final String VICTIM_SURCHARGE_REASON_TYPE = "victimSurchargeReasonType";
    public static final String REASON_FOR_NO_VICTIM_SURCHARGE = "reasonForNoVictimSurcharge";
    public static final String REASON_FOR_REDUCED_VICTIM_SURCHARGE = "reasonForReducedVictimSurcharge";
    public static final String REASON_FOR_NO_COSTS = "reasonForNoCosts";
    public static final String RESERVE_TERMS = "reserveTerms";
    public static final String LUMP_SUM_AMOUNT = "lumpSumAmount";
    public static final String LUMP_SUM = "lumpSum";
    public static final String COSTS = "costs";
    public static final String VICTIM_SURCHARGE = "victimSurcharge";
    public static final String PAYMENT_TERMS = "paymentTerms";


    // refer for court
    public static final String REASON_FOR_REFER_TO_COURT = "reasonForReferToCourt";
    public static final String HEARING_TYPE_ID = "hearingTypeId";
    public static final String REFERRAL_REASON_ID = "referralReasonId";
    public static final String ESTIMATED_HEARING_DURATION = "estimatedHearingDuration";
    public static final String LISTING_NOTES = "listingNotes";

    public static final String INSTALLMENT_AMOUNT = "installmentAmount";
    public static final String INSTALLMENT_PERIOD = "installmentPeriod";
    public static final String INSTALLMENT_START_DATE = "installmentStartDate";


    public static final String FINE_TRANSFERRED_TO = "fineTransferredTo";
    public static final String INSTALLMENTS = "installments";
    public static final String NOT_IMPOSED = "not imposed";
    public static final String REDUCED = "reduced";

    public static final String NATIONAL_COURT_CODE = "nationalCourtCode";
    public static final String NATIONAL_COURT_NAME = "nationalCourtName";
    public static final String REFERRED_TO_DATE_TIME = "referredToDateTime";

    public static final String DATE_OF_HEARING = "dateOfHearing";
    public static final String TIME_OF_HEARING = "timeOfHearing";
    public static final String MAGISTRATES_COURT = "magistratesCourt";
    public static final String REFERRED_TO_ROOM = "referredToRoom";
    public static final String REFERRED_TO_COURT = "referredToCourt";
    public static final String REASON = "reason";


    public static final String AMOUNT = "amount";
    public static final String WITHIN_DAYS = "withinDays";
    public static final String START_DATE = "startDate";
    public static final String PERIOD = "period";

    // verdict attributes
    public static final String PROVED_SJP = "PROVED_SJP";
    public static final String FOUND_GUILTY = "FOUND_GUILTY";
    public static final String FOUND_NOT_GUILTY = "FOUND_NOT_GUILTY";
    public static final String NO_VERDICT = "NO_VERDICT";

    // old schema attributes
    public static final String TERMINAL_ENTRIES = "terminalEntries";
    public static final String RESULTS = "results";
    public static final String INDEX = "index";
    public static final String VALUE = "value";
    public static final String CODE = "code";
    public static final String UNIT = "unit";

    // old result codes
    public static final String AD = "AD";
    public static final String CD = "CD";
    public static final String FCOMP = "FCOMP";
    public static final String NCR = "NCR";
    public static final String GPTAC = "GPTAC";

    public static final String FO = "FO";


    public static final String FCOST = "FCOST";
    public static final String FVS = "FVS";
    public static final String COLLO = "COLLO";
    public static final String NCOLLO = "NCOLLO";
    public static final String ABDC = "ABDC";
    public static final String AEOC = "AEOC";
    public static final String NOVS = "NOVS";
    public static final String NCOSTS = "NCOSTS";
    public static final String TFOOUT = "TFOOUT";
    public static final String SUMRCC = "SUMRCC";
    public static final String RLSUM = "RLSUM";
    public static final String RLSUMI = "RLSUMI";
    public static final String RINSTL = "RINSTL";
    public static final String LSUM = "LSUM";
    public static final String LSUMI = "LSUMI";
    public static final String INSTL = "INSTL";

    // legacy codes
    public static final String SUMRTO = "SUMRTO";

    public static final String NAME = "name";
    public static final String CASE_ID = "caseId";
    public static final String SESSION_ID = "sessionId";
    public static final String DECISION_ID = "decisionId";
    public static final String SAVED_AT = "savedAt";
    public static final String OFFENCE_DECISIONS = "offenceDecisions";
    public static final String CREATED = "created";
    public static final String SJP_SESSION_ID = "sjpSessionId";
    public static final String OFFENCES = "offences";
    public static final String FINANCIAL_IMPOSITION = "financialImposition";




}
