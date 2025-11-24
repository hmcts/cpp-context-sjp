package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "notification_of_endorsement_status")
public class NotificationOfEndorsementStatus implements Serializable {

    @Id
    @Column(name = "application_decision_id", unique = true, nullable = false)
    private UUID applicationDecisionId;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "updated", nullable = false)
    private ZonedDateTime updated;

    public NotificationOfEndorsementStatus() {
    }

    public NotificationOfEndorsementStatus(final UUID applicationDecisionId, final UUID fileId, final Status status, final ZonedDateTime updated) {
        this.applicationDecisionId = applicationDecisionId;
        this.fileId = fileId;
        this.status = status;
        this.updated = updated;
    }

    public UUID getApplicationDecisionId() {
        return applicationDecisionId;
    }

    public void setApplicationDecisionId(final UUID applicationDecisionId) {
        this.applicationDecisionId = applicationDecisionId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
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

    public void setUpdated(final ZonedDateTime updated) {
        this.updated = updated;
    }

    public enum Status {
        GENERATED,
        GENERATION_FAILED,
        QUEUED,
        FAILED,
        SENT
    }
}
