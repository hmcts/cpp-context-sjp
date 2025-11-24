package uk.gov.moj.cpp.sjp.event.processor.service;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class NotificationOfPartialAocpStatus {
    private UUID caseId;
    private NotificationOfPartialAocpStatus.Status status;
    private ZonedDateTime updated;

    public NotificationOfPartialAocpStatus(final UUID caseId, final NotificationOfPartialAocpStatus.Status status, final ZonedDateTime updated) {
        this.caseId = caseId;
        this.status = status;
        this.updated = updated;
    }

    public static NotificationOfPartialAocpStatus queued(final UUID caseId) {
        return new NotificationOfPartialAocpStatus(
                caseId,
                NotificationOfPartialAocpStatus.Status.QUEUED,
                ZonedDateTime.now()
        );
    }

    public UUID getCaseId() {
        return caseId;
    }

    public NotificationOfPartialAocpStatus.Status getStatus() {
        return status;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public enum Status {
        QUEUED,
        FAILED,
        SENT
    }
}
