package uk.gov.moj.sjp.it;

import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;

public class Constants {
    public static final String PUBLIC_EVENT_SELECTOR_CASE_CREATED = "public.sjp-case-created";
    public static final String EVENT_SELECTOR_CASE_COMPLETED = "sjp.events.case-completed";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "sjp.events.case-document-added";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS = "public.sjp.case-document-already-exists";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED = "public.sjp.case-document-uploaded";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "public.sjp.case-document-added";
    public static final String EVENT_SELECTOR_PLEA_UPDATED = PleaUpdated.EVENT_NAME;
    public static final String EVENT_SELECTOR_PLEA_CANCELLED = PleaCancelled.EVENT_NAME;
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_UPDATED = "public.sjp.plea-updated";
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED = "public.sjp.plea-cancelled";
    public static final String EVENT_SELECTOR_TRIAL_REQUESTED = "sjp.events.trial-requested";
    public static final String EVENT_SELECTOR_CASE_RECEIVED = CaseReceived.EVENT_NAME;
    public static final String PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED = "public.sjp.all-offences-withdrawal-requested";
    public static final String SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED = AllOffencesWithdrawalRequested.EVENT_NAME;
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
    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "public.event";
    public static final String PRIVATE_ACTIVE_MQ_TOPIC = "sjp.event";
    public static final Integer MESSAGE_QUEUE_TIMEOUT = 15000;
    public static final int NOTICE_PERIOD_IN_DAYS = 28;

}