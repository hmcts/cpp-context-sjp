package uk.gov.moj.cpp.sjp.query.view.util;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.format.DateTimeFormatter;

public class CaseResultsConstants {

    public static final String ID = "id";
    public static final String VERDICT = "verdict";
    public static final String CASE_ID = "caseId";
    public static final String OFFENCE_ID = "offenceId";
    public static final String SESSION_ID = "sessionId";
    public static final String USER_ID = "userId";
    public static final String ASSIGNEE_ID = "assigneeId";
    public static final String PLEA = "plea";
    public static final String PLEA_METHOD = "pleaMethod";
    public static final String UPDATED_DATE = "updatedDate";
    public static final String PLEAD_DATE = "pleadDate";
    public static final String MITIGATION = "mitigation";
    public static final String POSTING_DATE = "postingDate";
    public static final String CASE_DOCUMENT = "caseDocument";
    public static final String MATERIAL_ID = "materialId";
    public static final String DOCUMENT_REFERENCE = "documentReference";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String REASON = "reason";
    public static final String MARKED_AT = "markedAt";
    public static final String CASE_ASSIGNMENT_TYPE = "caseAssignmentType";
    public static final String DATES_TO_AVOID = "datesToAvoid";
    public static final String EXPECTED_DATE_READY = "expectedDateReady";
    public static final String RESULTS = "results";

    public static final String WITHDRAW_RESULT_CODE = "WDRNNOT";
    public static final String DISMISS_RESULT_CODE = "D";
    public static final String ABSOLUTE_DISCHARGE_CODE = "AD";
    public static final String CONDITIONAL_DISCHARGE_CODE = "CD";
    public static final String FINE_CODE = "FO";
    public static final String BACK_DUTY_CODE = "FVEBD";
    public static final String EXCISE_PENALTY_CODE = "EXPEN";

    public static final String CODE = "code";
    public static final String RESULT_TYPE_ID = "resultTypeId";
    public static final String TERMINAL_ENTRIES = "terminalEntries";

    public static final String UNIT = "unit";
    public static final String LENGTH = "length";
    public static final String VALUE = "value";
    public static final String DAYS = "Days";
    public static final String DAY = "Day(s)";
    public static final String WEEKS = "Weeks";
    public static final String WEEK = "Week(s)";
    public static final String MONTHS = "Months";
    public static final String MONTH = "Month(s)";
    public static final String YEARS = "Years";
    public static final String YEAR = "Year(s)";
    public static final String INDEX = "index";
    public static final String PROMPT_DEFINITION_ID = "promptDefinitionId";
    public static final String ADDRESS = "address";
    public static final Integer FOURTEEN_DAYS = 14;
    public static final Integer TWENTY_EIGHT_DAYS = 28;
    public static final String LUMP_SUM_28_DAYS = "lump sum 28 days";
    public static final String LUMP_SUM_WITHIN_28_DAYS = "Lump sum within 28 days";
    public static final String LUMP_SUM_14_DAYS = "lump sum 14 days";
    public static final String LUMP_SUM_WITHIN_14_DAYS = "Lump sum within 14 days";
    public static final String ATTACHMENT_OF_EARNINGS = "Attachment of earnings";
    public static final String ATTACH_TO_EARNINGS = "ATTACH_TO_EARNINGS";
    public static final String DEDUCTIONS_FROM_BENEFIT = "Deductions from benefit";
    public static final String PAY_TO_COURT = "PAY_TO_COURT";
    public static final String DEDUCT_FROM_BENEFITS = "DEDUCT_FROM_BENEFITS";
    public static final String COMPENSATION_ORDERED = "COMPENSATION_ORDERED";
    public static final String DEFENDANT_KNOWN_DEFAULTER = "DEFENDANT_KNOWN_DEFAULTER";
    public static final String DEFENDANT_REQUESTED = "DEFENDANT_REQUESTED";
    public static final String MAKE_PAYMENTS_AS_ORDERED = "Make payments as ordered";
    public static final String PAY_DIRECTLY_TO_COURT = "Pay directly to court";
    public static final String CASE_UNSUITABLE_FOR_SJP = "Case unsuitable for SJP";
    public static final String FOR_DISQUALIFICATION_DEFENDANT_TO_ATTEND = "For disqualification - defendant to attend";
    public static final String FOR_SENTENCING_HEARING_DEFENDANT_TO_ATTEND = "For sentencing hearing - defendant to attend";
    public static final String FOR_A_CASE_MANAGEMENT_HEARING_NO_NEED_FOR_DEFENDANT_TO_ATTEND = "For a case management hearing - No need for defendant to attend";
    public static final String FOR_A_CASE_MANAGEMENT_HEARING_DEFENDANT_TO_ATTEND = "For a case management hearing - Defendant to attend";
    public static final String FOR_TRIAL = "For trial";
    public static final String DEFENCE_REQUEST = "Defence request";
    public static final String EQUIVOCAL_PLEA_FOR_TRIAL = "Equivocal plea - For trial";
    public static final String EQUIVOCAL_PLEA_DEFENDANT_TO_ATTEND_TO_CLARIFY_PLEA = "Equivocal plea - Defendant to attend to clarify plea";

    public static final String COSTS_AND_SURCHARGE = "costsAndSurcharge";
    public static final String PAYMENT = "payment";
    public static final String OFFENCE_DECISION_INFORMATION = "offenceDecisionInformation";

    public static final String TYPE = "type";
    public static final String WITHDRAW_REASON_ID = "withdrawalReasonId";
    public static final String REFERRAL_REASON_ID = "referralReasonId";
    public static final String ADJOURN_TO = "adjournTo";
    public static final String DISCHARGE_TYPE = "dischargeType";

    public static final String BACK_DUTY = "backDuty";
    public static final String EXCISE_PENALTY = "excisePenalty";

    public static final String DISCHARGE_FOR = "dischargedFor";
    public static final String COMPENSATION = "compensation";
    public static final String NO_COMPENSATION_REASON = "noCompensationReason";
    public static final String FINE = "fine";

    public static final String TOTAL_SUM = "totalSum";
    public static final String PAYMENT_TYPE = "paymentType";
    public static final String REASON_WHY_NOT_ATTACHED_OR_DEDUCTED = "reasonWhyNotAttachedOrDeducted";
    public static final String REASON_FOR_DEDUCTING_FROM_BENEFITS = "reasonForDeductingFromBenefits";
    public static final String PAYMENT_TERMS = "paymentTerms";
    public static final String FINE_TRANSFERRED_TO = "fineTransferredTo";

    public static final String NATIONAL_COURT_CODE = "nationalCourtCode";
    public static final String NATIONAL_COURT_NAME = "nationalCourtName";
    public static final String RESERVE_TERMS = "reserveTerms";
    public static final String LUMP_SUM = "lumpSum";
    public static final String INSTALLMENTS = "installments";

    public static final String AMOUNT = "amount";
    public static final String START_DATE = "startDate";
    public static final String WITHIN_DAYS = "withinDays";
    public static final String PAY_BY_DATE = "payByDate";
    
    public static final String FINANCIAL_COSTS_CODE = "FCOST";
    public static final String NO_COSTS_CODE = "NCOSTS";
    public static final String COLLECTION_ORDER_CODE = "COLLO";
    public static final String VICTIM_SURCHARGE_CODE = "FVS";
    public static final String NO_VICTIM_SURCHARGE_CODE = "NOVS";
    public static final String LUMP_SUM_CODE = "LSUM";
    public static final String LUMP_SUM_PLUS_INSTALLMENTS_CODE = "LSUMI";
    public static final String RESERVE_TERMS_LUMP_SUM_CODE = "RLSUM";
    public static final String RESERVE_TERMS_LUMP_SUM_PLUS_INSTALLMENTS_CODE = "RLSUMI";
    public static final String INSTALLMENTS_CODE = "INSTL";
    public static final String RESERVE_TERMS_INSTALLMENTS_CODE = "RINSTL";
    public static final String APPLICATION_MADE_FOR_BENEFIT_DEDUCTIONS = "ABDC";
    public static final String ATTACHMENT_OF_EARNINGS_ORDER = "AEOC";

    public static final String PROVED_SJP = "PROVED_SJP";
    public static final String PROVED_SJP_NAME = "provedSJP";

    public static final String REFERRED_FOR_FUTURE_SJP_SESSION_RESULT_CODE = "RSJP";
    public static final String REFERRED_TO_OPEN_COURT_RESULT_CODE = "SUMRTO";
    public static final String REFER_FOR_COURT_HEARING_RESULT_CODE = "SUMRCC";


    public static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter HOUR_FORMAT = ofPattern("hh:mm a");

    public static final String COMPENSATION_CODE = "FCOMP";
    public static final String NO_COMPENSATION_REASON_CODE = "NCR";
    public static final DateTimeFormatter TERMINAL_ENTRIES_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    public static final String DISCHARGE_FOR_DAY = "DAY";
    public static final String DISCHARGE_FOR_WEEK = "WEEK";
    public static final String DISCHARGE_FOR_MONTH = "MONTH";
    public static final String DISCHARGE_FOR_YEAR = "YEAR";

    public static final String OFFENCE_DECISIONS = "offenceDecisions";
    public static final String FINANCIAL_IMPOSITION = "financialImposition";
    public static final String VICTIM_SURCHARGE = "victimSurcharge";
    public static final String COLLECTION_ORDER_MADE = "collectionOrderMade";
    public static final String ABSOLUTE = "ABSOLUTE";
    public static final String CONDITIONAL = "CONDITIONAL";
    public static final String PERIOD = "period";
    public static final String COSTS = "costs";
    public static final String REFERRED_TO_DATE_TIME = "referredToDateTime";
    public static final String REFERRED_TO_COURT = "referredToCourt";
    public static final String MAGISTRATES_COURT = "magistratesCourt";
    public static final String REFERRED_TO_ROOM = "referredToRoom";
    public static final String CREATED = "created";
    public static final String ACCOUNT_DIVISION_CODE = "accountDivisionCode";
    public static final String ENFORCING_COURT_CODE = "enforcingCourtCode";
    public static final String OFFENCES = "offences";
    public static final String INTERIM = "interim";

    public static final String DEFENDANT = "defendant";
    public static final String PERSONAL_DETAILS = "personalDetails";
    public static final String POSTCODE = "postcode";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String REASON_FOR_NO_COSTS = "reasonForNoCosts";
    public static final String REASON_FOR_NO_VICTIM_SURCHARGE = "reasonForNoVictimSurcharge";

    private CaseResultsConstants() {
    }
}
