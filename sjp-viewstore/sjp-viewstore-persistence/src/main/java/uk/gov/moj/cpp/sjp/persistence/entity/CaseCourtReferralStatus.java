package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.google.common.base.Objects;

@Entity
@Table(name = "case_court_referral_status")
public class CaseCourtReferralStatus {

    @Column(name = "case_id")
    @Id
    private UUID caseId;

    @Column(name = "requested_at")
    private ZonedDateTime requestedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "rejected_at")
    private ZonedDateTime rejectedAt;

    public CaseCourtReferralStatus() {
    }

    public CaseCourtReferralStatus(final UUID caseId, final ZonedDateTime requestedAt) {
        this(caseId, requestedAt, null, null);
    }

    public CaseCourtReferralStatus(final UUID caseId, final ZonedDateTime requestedAt, final ZonedDateTime rejectedAt, final String rejectionReason) {
        this.caseId = caseId;
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

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(final ZonedDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ZonedDateTime getRejectedAt() {
        return rejectedAt;
    }

    @SuppressWarnings("squid:S00121")
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CaseCourtReferralStatus that = (CaseCourtReferralStatus) o;
        return Objects.equal(caseId, that.caseId) &&
                Objects.equal(requestedAt, that.requestedAt) &&
                Objects.equal(rejectionReason, that.rejectionReason) &&
                Objects.equal(rejectedAt, that.rejectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(caseId, requestedAt, rejectionReason, rejectedAt);
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void markRejected(final ZonedDateTime rejectedAt, final String rejectionReason) {
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
    }


}
