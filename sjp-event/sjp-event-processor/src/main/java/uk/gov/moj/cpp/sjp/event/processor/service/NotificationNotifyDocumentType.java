package uk.gov.moj.cpp.sjp.event.processor.service;

import static java.lang.String.format;

import java.util.stream.Stream;

public enum NotificationNotifyDocumentType {
    ENDORSEMENT_REMOVAL_NOTIFICATION,
    ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION,
    PARTIAL_AOCP_CRITERIA_NOTIFICATION,
    AOCP_ACCEPTED_EMAIL_NOTIFICATION;

    public static NotificationNotifyDocumentType fromString(final String documentType) {
        return Stream.of(values()).filter(e -> e.name().equalsIgnoreCase(documentType)).findAny()
                .orElseThrow(() -> new IllegalArgumentException(format("Invalid Document type %s", documentType)));
    }
}