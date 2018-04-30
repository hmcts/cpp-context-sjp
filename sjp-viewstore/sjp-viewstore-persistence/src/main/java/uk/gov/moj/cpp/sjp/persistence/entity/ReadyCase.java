package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.persistence.entity.view.ReadyCasesReasonCount;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

@SqlResultSetMapping(name = "caseReasonsCountMapping", classes = @ConstructorResult(targetClass = ReadyCasesReasonCount.class, columns = {@ColumnResult(name = "reason"), @ColumnResult(name = "count", type = Long.class)}))
@NamedNativeQuery(name = "readyCases.readyCasesReasonCounts", query = "SELECT reason reason, COUNT(*) count FROM ready_cases GROUP BY reason", resultSetMapping = "caseReasonsCountMapping")
@Entity
@Table(name = "ready_cases")
public class ReadyCase {

    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "reason")
    @Enumerated(EnumType.STRING)
    private CaseReadinessReason reason;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    public ReadyCase() {
        //required for hibernate
    }

    public ReadyCase(final UUID caseId, final CaseReadinessReason reason) {
        this.caseId = caseId;
        this.reason = reason;
    }

    public ReadyCase(final UUID caseId, final CaseReadinessReason reason, final UUID assigneeId) {
        this.caseId = caseId;
        this.reason = reason;
        this.assigneeId = assigneeId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CaseReadinessReason getReason() {
        return reason;
    }

    public Optional<UUID> getAssigneeId() {
        return Optional.ofNullable(assigneeId);
    }

    public void setAssigneeId(final UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReadyCase readyCase = (ReadyCase) o;
        return Objects.equals(caseId, readyCase.caseId) &&
                reason == readyCase.reason &&
                Objects.equals(assigneeId, readyCase.assigneeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, reason, assigneeId);
    }
}
