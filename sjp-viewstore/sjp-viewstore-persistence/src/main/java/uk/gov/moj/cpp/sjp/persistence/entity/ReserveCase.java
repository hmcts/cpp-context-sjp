package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "reserve_case", uniqueConstraints = @UniqueConstraint(columnNames = "id"))
public class ReserveCase implements Serializable {

    @Column(name = "id", nullable = false, unique = true)
    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "case_urn")
    private String caseUrn;

    @Column(name = "reserved_by")
    private UUID reservedBy;

    @Column(name = "reserved_at")
    private ZonedDateTime reservedAt;

    public ReserveCase() {
       this.id = UUID.randomUUID();
    }

    public ReserveCase(final UUID caseId,
                       final String caseUrn,
                       final UUID reservedBy,
                       final ZonedDateTime reservedAt) {
        this.id = UUID.randomUUID();
        this.caseId = caseId;
        this.caseUrn = caseUrn;
        this.reservedBy = reservedBy;
        this.reservedAt = reservedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public UUID getReservedBy() {
        return reservedBy;
    }

    public void setReservedBy(final UUID reservedBy) {
        this.reservedBy = reservedBy;
    }

    public ZonedDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(final ZonedDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReserveCase reserveCase = (ReserveCase) o;
        return Objects.equals(caseId, reserveCase.getCaseId()) &&
                Objects.equals(reservedAt, reserveCase.reservedAt) &&
                Objects.equals(reservedBy, reserveCase.reservedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCaseId(), reservedAt, reservedBy);
    }
}
