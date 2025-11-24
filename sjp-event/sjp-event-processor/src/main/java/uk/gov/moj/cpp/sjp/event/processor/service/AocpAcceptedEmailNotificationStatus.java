package uk.gov.moj.cpp.sjp.event.processor.service;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class AocpAcceptedEmailNotificationStatus implements Serializable {

    private UUID caseId;
    private Status status;
    private ZonedDateTime updated;

    public AocpAcceptedEmailNotificationStatus() {
    }

    public AocpAcceptedEmailNotificationStatus(final UUID caseId, final Status status, final ZonedDateTime updated) {
        this.caseId = caseId;
        this.status = status;
        this.updated = updated;
    }

    public static AocpAcceptedEmailNotificationStatus queued(final UUID caseId) {
        return new AocpAcceptedEmailNotificationStatus(
                caseId,
                AocpAcceptedEmailNotificationStatus.Status.QUEUED,
                ZonedDateTime.now()
        );
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (!(o instanceof AocpAcceptedEmailNotificationStatus)) {
            return false;
        }
        final AocpAcceptedEmailNotificationStatus that = (AocpAcceptedEmailNotificationStatus) o;
        return Objects.equals(getCaseId(), that.getCaseId()) && getStatus() == that.getStatus() && Objects.equals(getUpdated(), that.getUpdated());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCaseId(), getStatus(), getUpdated());
    }

    public void setUpdated(final ZonedDateTime updated) {
        this.updated = updated;
    }

    public enum Status {
        QUEUED,
        FAILED,
        SENT
    }
}
