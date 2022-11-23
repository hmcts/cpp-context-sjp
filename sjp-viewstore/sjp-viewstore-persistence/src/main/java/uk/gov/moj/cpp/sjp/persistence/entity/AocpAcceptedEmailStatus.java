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
@Table(name = "accepted_email_notification")
public class AocpAcceptedEmailStatus implements Serializable {

    @Id
    @Column(name = "case_id", unique = true, nullable = false)
    private UUID caseId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "updated", nullable = false)
    private ZonedDateTime updated;

    public AocpAcceptedEmailStatus() {
    }

    public AocpAcceptedEmailStatus(final UUID caseId, final Status status, final ZonedDateTime updated) {
        this.caseId = caseId;
        this.status = status;
        this.updated = updated;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
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

    public void setUpdated(final ZonedDateTime updated) {
        this.updated = updated;
    }

    public enum Status {
        NOT_INITIATED,
        QUEUED,
        FAILED,
        SENT
    }
}
