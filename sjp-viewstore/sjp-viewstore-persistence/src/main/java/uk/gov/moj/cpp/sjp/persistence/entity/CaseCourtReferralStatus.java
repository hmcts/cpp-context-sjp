package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;

@Entity
@Table(name = "case_court_referral_status")
public class CaseCourtReferralStatus {

    @Column(name = "case_id")
    @Id
    private UUID caseId;

    @Column(name = "urn")
    private String urn;

    @Column(name = "requested_at")
    private ZonedDateTime requestedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "rejected_at")
    private ZonedDateTime rejectedAt;

    public CaseCourtReferralStatus() {
    }

    public CaseCourtReferralStatus(final UUID caseId, final String urn, final ZonedDateTime requestedAt) {
        this(caseId, urn, requestedAt, null, null);
    }

    public CaseCourtReferralStatus(final UUID caseId, final String urn, final ZonedDateTime requestedAt, final ZonedDateTime rejectedAt, final String rejectionReason) {
        this.caseId = caseId;
        this.urn = urn;
        this.requestedAt = requestedAt;
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getUrn() { return urn; }

    public void setUrn(final String urn) { this.urn = urn; }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(final ZonedDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ZonedDateTime getRejectedAt() {
        return rejectedAt;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, urn, requestedAt, rejectionReason, rejectedAt);
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void markRejected(final ZonedDateTime rejectedAt, final String rejectionReason) {
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
    }


}
