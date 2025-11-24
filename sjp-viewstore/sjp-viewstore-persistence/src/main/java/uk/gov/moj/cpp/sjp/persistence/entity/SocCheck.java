package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "soc_check")
public class SocCheck {

    private static final long serialVersionUID = 4017199970773938902L;

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "checked_by", nullable = false)
    private UUID checkedBy;

    @Column(name = "checked_at", nullable = false)
    private ZonedDateTime checkedAt;

    public SocCheck(final UUID id, final UUID caseId, final UUID checkedBy, final ZonedDateTime checkedAt) {
        this.id = id;
        this.caseId = caseId;
        this.checkedBy = checkedBy;
        this.checkedAt = checkedAt;
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

    public UUID getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(final UUID checkedBy) {
        this.checkedBy = checkedBy;
    }

    public ZonedDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(final ZonedDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

}
