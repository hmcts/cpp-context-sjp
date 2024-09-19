package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@SuppressWarnings("squid:S107")
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

    @Column(name = "session_type")
    @Enumerated(EnumType.STRING)
    private SessionType sessionType;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "prosecuting_authority")
    private String prosecutionAuthority;

    @Column(name = "posting_date")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate postingDate;

    @Column(name = "marked_ready_date")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate markedAt;

    public ReadyCase() {
        //required for hibernate
    }

    public ReadyCase(final UUID caseId,
                     final CaseReadinessReason reason,
                     final UUID assigneeId,
                     final SessionType sessionType,
                     final Integer priority,
                     final String prosecutionAuthority,
                     final LocalDate postingDate,
                     final LocalDate markedAt) {
        this.caseId = caseId;
        this.reason = reason;
        this.assigneeId = assigneeId;
        this.sessionType = sessionType;
        this.priority = priority;
        this.prosecutionAuthority = prosecutionAuthority;
        this.postingDate = postingDate;
        this.markedAt = markedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setReason(final CaseReadinessReason reason) {
        this.reason = reason;
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

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(final SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public String getProsecutionAuthority() {
        return prosecutionAuthority;
    }

    public void setProsecutionAuthority(final String prosecutionAuthority) {
        this.prosecutionAuthority = prosecutionAuthority;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public LocalDate getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(final LocalDate markedAt) {
        this.markedAt = markedAt;
    }

    @Override
    public boolean equals(final Object o) {
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
