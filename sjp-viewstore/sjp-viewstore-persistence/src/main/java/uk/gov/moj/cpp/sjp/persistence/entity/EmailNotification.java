package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "email_notification")
public class EmailNotification {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "reference_id", unique = true, nullable = false)
    private UUID referenceId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailNotification.Status status;

    @Column(name = "updated", nullable = false)
    private ZonedDateTime updated;

    @Column(name = "notification_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailNotification.NotificationNotifyDocumentType notificationType;

    public EmailNotification() {
    }

    public EmailNotification(final UUID id, final UUID referenceId, final EmailNotification.Status status, final ZonedDateTime updated,
                             final EmailNotification.NotificationNotifyDocumentType notificationType) {
        this.id = id;
        this.referenceId = referenceId;
        this.status = status;
        this.updated = updated;
        this.notificationType = notificationType;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(final UUID referenceId) {
        this.referenceId = referenceId;
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

    public NotificationNotifyDocumentType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(final NotificationNotifyDocumentType notificationType) {
        this.notificationType = notificationType;
    }

    public enum Status {
        QUEUED,
        FAILED,
        SENT
    }

    public enum NotificationNotifyDocumentType {
        ENDORSEMENT_REMOVAL_NOTIFICATION,
        ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION,
        PARTIAL_AOCP_CRITERIA_NOTIFICATION;
    }
}
