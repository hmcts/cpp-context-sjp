package uk.gov.moj.sjp.it;

public class Constants {
    public static final String PUBLIC_EVENT_SELECTOR_DELETE_CASE_DOCUMENT_REQUEST_ACCEPTED = "public.sjp.delete-case-document-request-accepted";
    public static final String PUBLIC_EVENT_SELECTOR_DELETE_CASE_DOCUMENT_REQUEST_REJECTED = "public.sjp.delete-case-document-request-rejected";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS = "public.sjp.case-document-already-exists";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED = "public.sjp.case-document-uploaded";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "public.sjp.case-document-added";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOAD_REJECTED = "public.sjp.events.case-document-upload-rejected";
    public static final String EVENT_OFFENCES_WITHDRAWAL_STATUS_SET = "sjp.events.offences-withdrawal-status-set";
    public static final String PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET = "public.sjp.offences-withdrawal-status-set";
    public static final String PUBLIC_CASE_RESERVED = "public.sjp.case-reserved";
    public static final String PUBLIC_CASE_ALREADY_RESERVED = "public.sjp.case-already-reserved";
    public static final String PUBLIC_CASE_UNRESERVED = "public.sjp.case-unreserved";
    public static final String PUBLIC_CASE_ALREADY_UNRESERVED = "public.sjp.case-already-unreserved";
    public static final String PUBLIC_SJP_CASE_UPDATE_REJECTED = "public.sjp.case-update-rejected";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA = "public.sjp.case-reopened-in-libra";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED = "public.sjp.case-reopened-in-libra-updated";
    public static final String PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE = "public.sjp.case-reopened-in-libra-undone";
    public static final String CASE_NOTE_ADDED_EVENT = "sjp.events.case-note-added";
    public static final String CASE_ADJOURNED_TO_LATER_SJP_EVENT = "sjp.events.case-adjourned-to-later-sjp-hearing-recorded";

    public static final String PUBLIC_EVENT_SET_PLEAS = "public.sjp.pleas-set";

    public static final String PUBLIC_EVENT = "public.event";
    public static final String SJP_EVENT = "sjp.event";
    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "jms.topic.public.event";
    public static final String PRIVATE_ACTIVE_MQ_TOPIC = "jms.topic.sjp.event";

    public static final Integer MESSAGE_QUEUE_TIMEOUT = 15000;
    public static final int NOTICE_PERIOD_IN_DAYS = 28;
    public static final String DEFAULT_OFFENCE_CODE = "PS00001";
    public static final Integer OFFENCE_DATE_CODE_FOR_BETWEEN = 4;

    public static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";

    public static final String DEFENDANT_PENDING_CHANGES_ACCEPTED_PUBLIC_EVENT = "public.sjp.events.defendant-pending-changes-accepted";
    public static final String DEFENDANT_PENDING_CHANGES_REJECTED_PUBLIC_EVENT = "public.sjp.events.defendant-pending-changes-rejected";
}
