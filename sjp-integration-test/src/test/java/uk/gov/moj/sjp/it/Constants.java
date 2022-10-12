package uk.gov.moj.sjp.it;

import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;

public class Constants {
    public static final String PUBLIC_EVENT_SELECTOR_CASE_CREATED = "public.sjp-case-created";
    public static final String EVENT_SELECTOR_CASE_COMPLETED = "sjp.events.case-completed";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "sjp.events.case-document-added";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS = "public.sjp.case-document-already-exists";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED = "public.sjp.case-document-uploaded";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_UPLOAD_REJECTED = "sjp.events.case-document-upload-rejected";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "public.sjp.case-document-added";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOAD_REJECTED = "public.sjp.events.case-document-upload-rejected";
    public static final String EVENT_SELECTOR_PLEA_UPDATED = PleaUpdated.EVENT_NAME;
    public static final String EVENT_SELECTOR_PLEA_CANCELLED = PleaCancelled.EVENT_NAME;
    public static final String PUBLIC_EVENT_SELECTOR_PLEAS_SET = "public.sjp.pleas-set";
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED = "public.sjp.plea-cancelled";
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_UPDATED = "public.sjp.plea-updated";
    public static final String EVENT_SELECTOR_TRIAL_REQUESTED = "sjp.events.trial-requested";
    public static final String EVENT_SELECTOR_DATES_TO_AVOID_ADDED = "sjp.events.dates-to-avoid-added";
    public static final String EVENT_SELECTOR_CASE_RECEIVED = CaseReceived.EVENT_NAME;
    public static final String PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED = "public.sjp.all-offences-withdrawal-requested";
    public static final String SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED = AllOffencesWithdrawalRequested.EVENT_NAME;
    public static final String EVENT_OFFENCES_WITHDRAWAL_STATUS_SET = "sjp.events.offences-withdrawal-status-set";
    public static final String PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET = "public.sjp.offences-withdrawal-status-set";
    public static final String PUBLIC_EVENT_SJP_PENDING_CASES_PUBLIC_LIST_GENERATED = "public-sjp-pending-cases-public-list-generated";
    public static final String PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED = "public.sjp.all-offences-withdrawal-request-cancelled";
    public static final String SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED = AllOffencesWithdrawalRequestCancelled.EVENT_NAME;
    public static final String PUBLIC_SJP_CASE_UPDATE_REJECTED = "public.sjp.case-update-rejected";
    public static final String SJP_EVENTS_CASE_UPDATE_REJECTED = CaseUpdateRejected.EVENT_NAME;
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA = "sjp.events.case-reopened-in-libra";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA = "public.sjp.case-reopened-in-libra";
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED = "sjp.events.case-reopened-in-libra-updated";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED = "public.sjp.case-reopened-in-libra-updated";
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE = "sjp.events.case-reopened-in-libra-undone";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE = "public.sjp.case-reopened-in-libra-undone";
    public static final String EVENT_CASE_MARKED_READY_FOR_DECISION = CaseMarkedReadyForDecision.EVENT_NAME;
    public static final String EVENT_CASE_ASSIGNMENT_REJECTED = CaseAssignmentRejected.EVENT_NAME;
    public static final String EVENT_CASE_REFERRED_FOR_COURT_HEARING = "sjp.events.case-referred-for-court-hearing";
    public static final String CASE_NOTE_ADDED_EVENT = "sjp.events.case-note-added";
    public static final String CASE_ADJOURNED_TO_LATER_SJP_EVENT = "sjp.events.case-adjourned-to-later-sjp-hearing-recorded";

    public static final String PUBLIC_EVENT_CASE_ASSIGNMENT_REJECTED = "public.sjp.case-assignment-rejected";
    public static final String PUBLIC_EVENT_SET_PLEAS = "public.sjp.pleas-set";

    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "public.event";
    public static final String PRIVATE_ACTIVE_MQ_TOPIC = "sjp.event";
    public static final String COMMAND_HANDLE_ACTIVE_MQ_QUEUE = "sjp.handler.command";

    public static final Integer MESSAGE_QUEUE_TIMEOUT = 15000;
    public static final int NOTICE_PERIOD_IN_DAYS = 28;
    public static final String DEFAULT_OFFENCE_CODE = "PS00001";
    public static final String DEFAULT_OFFENCE_TITLE = "Postal service - convey a letter without a licence";
    public static final Integer OFFENCE_DATE_CODE_FOR_BETWEEN = 4;

    public static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";
}
