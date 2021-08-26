package uk.gov.moj.cpp.sjp.event.processor.service;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class EnforcementPendingApplicationNotificationStatus implements Serializable {

    private UUID applicationId;
    private UUID fileId;
    private Status status;
    private ZonedDateTime updated;

    public EnforcementPendingApplicationNotificationStatus() {
    }

    public EnforcementPendingApplicationNotificationStatus(final UUID applicationId, final UUID fileId, final Status status, final ZonedDateTime updated) {
        this.applicationId = applicationId;
        this.fileId = fileId;
        this.status = status;
        this.updated = updated;
    }

    public static EnforcementPendingApplicationNotificationStatus queued(final UUID applicationId) {
        return new EnforcementPendingApplicationNotificationStatus(
                applicationId,
                null,
                EnforcementPendingApplicationNotificationStatus.Status.QUEUED,
                ZonedDateTime.now()
        );
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
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
        if (!(o instanceof EnforcementPendingApplicationNotificationStatus)) {
            return false;
        }
        final EnforcementPendingApplicationNotificationStatus that = (EnforcementPendingApplicationNotificationStatus) o;
        return Objects.equals(getApplicationId(), that.getApplicationId()) && Objects.equals(getFileId(), that.getFileId()) && getStatus() == that.getStatus() && Objects.equals(getUpdated(), that.getUpdated());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApplicationId(), getFileId(), getStatus(), getUpdated());
    }

    public void setUpdated(final ZonedDateTime updated) {
        this.updated = updated;
    }

    public enum Status {
        NOT_INITIATED,
        INITIATED,
        GENERATED,
        GENERATION_FAILED,
        QUEUED,
        FAILED,
        SENT
    }
}
