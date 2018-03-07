package uk.gov.moj.sjp.it;

import uk.gov.moj.cpp.sjp.event.CaseReceived;

public class EventSelector {
    public static final String EVENT_SELECTOR_CASE_COMPLETED = "sjp.events.case-completed";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "sjp.events.case-document-added";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS = "public.sjp.case-document-already-exists";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED = "public.sjp.case-document-uploaded";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "public.sjp.case-document-added";
    public static final String EVENT_SELECTOR_PLEA_UPDATED = "sjp.events.plea-updated";
    public static final String EVENT_SELECTOR_PLEA_CANCELLED = "sjp.events.plea-cancelled";
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_UPDATED = "public.sjp.plea-updated";
    public static final String PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED = "public.sjp.plea-cancelled";
    public static final String EVENT_SELECTOR_CASE_RECEIVED = CaseReceived.EVENT_NAME;
    public static final String PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED = "public.sjp.all-offences-withdrawal-requested";
    public static final String SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED = "sjp.events.all-offences-withdrawal-requested";
    public static final String PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED = "public.sjp.all-offences-withdrawal-request-cancelled";
    public static final String SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED = "sjp.events.all-offences-withdrawal-request-cancelled";
    public static final String PUBLIC_SJP_CASE_UPDATE_REJECTED = "public.sjp.case-update-rejected";
    public static final String SJP_EVENTS_CASE_UPDATE_REJECTED = "sjp.events.case-update-rejected";
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA = "sjp.events.case-reopened-in-libra";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA = "public.sjp.case-reopened-in-libra";
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED = "sjp.events.case-reopened-in-libra-updated";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED = "public.sjp.case-reopened-in-libra-updated";
    public static final String EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE = "sjp.events.case-reopened-in-libra-undone";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE = "public.sjp.case-reopened-in-libra-undone";
    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "public.event";
}