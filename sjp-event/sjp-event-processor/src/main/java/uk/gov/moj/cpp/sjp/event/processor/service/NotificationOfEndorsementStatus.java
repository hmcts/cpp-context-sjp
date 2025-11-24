package uk.gov.moj.cpp.sjp.event.processor.service;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class NotificationOfEndorsementStatus {

    private UUID applicationDecisionId;
    private UUID fileId;
    private Status status;
    private ZonedDateTime updated;

    public NotificationOfEndorsementStatus(final UUID applicationDecisionId, final UUID fileId, final Status status, final ZonedDateTime updated) {
        this.applicationDecisionId = applicationDecisionId;
        this.fileId = fileId;
        this.status = status;
        this.updated = updated;
    }

    public static NotificationOfEndorsementStatus queued(final UUID applicationDecisionId) {
        return new NotificationOfEndorsementStatus(
                applicationDecisionId,
                null,
                Status.QUEUED,
                ZonedDateTime.now()
        );
    }

    public UUID getApplicationDecisionId() {
        return applicationDecisionId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public Status getStatus() {
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
        GENERATED,
        GENERATION_FAILED,
        QUEUED,
        FAILED,
        SENT
    }
}
