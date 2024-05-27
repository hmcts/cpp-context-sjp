package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

public enum TemplateIdentifier {
    ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION("EnforcementPendingApplicationNotification"),
    NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT("NotificationToDvlaToRemoveEndorsement"),
    TRANSPARENCY_REPORT_ENGLISH("PendingCasesFullEnglish"),
    TRANSPARENCY_REPORT_ENGLISH_DELTA("PendingCasesDeltaEnglish"),
    TRANSPARENCY_REPORT_WELSH("PendingCasesFullWelsh"),
    TRANSPARENCY_REPORT_WELSH_DELTA("PendingCasesDeltaWelsh"),
    PRESS_TRANSPARENCY_REPORT("PressPendingCasesFullEnglish"),
    PRESS_TRANSPARENCY_DELTA_REPORT("PressPendingCasesDeltaEnglish"),
    AOCP_ACCEPTED_EMAIL_NOTIFICATION("aocpAcceptedEmailNotification");

    private final String value;

    TemplateIdentifier(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
