package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table(name = "offence_decision")
@IdClass(OffenceDecisionId.class)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "decision_type")
public abstract class OffenceDecision {

    @Id
    @Column(name = "offence_id")
    private UUID offenceId;

    @Id
    @Column(name = "case_decision_id")
    private UUID caseDecisionId;

    @Column(name = "decision_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private DecisionType decisionType;

    @Column(name = "verdict_type")
    @Enumerated(EnumType.STRING)
    private VerdictType verdictType;

    @Column(name = "plea_at_decision_time")
    @Enumerated(EnumType.STRING)
    private PleaType pleaAtDecisionTime;

    @Column(name = "plea_date")
    private ZonedDateTime pleaDate;

    @Column(name = "conviction_date")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate convictionDate;

    protected OffenceDecision() {
    }

    protected OffenceDecision(final UUID offenceId, final UUID caseDecisionId,
                              final DecisionType decisionType, final VerdictType verdictType,
                              final LocalDate convictionDate) {
        this.offenceId = offenceId;
        this.caseDecisionId = caseDecisionId;
        this.decisionType = decisionType;
        this.verdictType = verdictType;
        this.convictionDate = convictionDate;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public UUID getCaseDecisionId() {
        return caseDecisionId;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public VerdictType getVerdictType() {
        return verdictType;
    }

    public PleaType getPleaAtDecisionTime() {
        return pleaAtDecisionTime;
    }

    public void setPleaAtDecisionTime(PleaType pleaAtDecisionTime) {
        this.pleaAtDecisionTime = pleaAtDecisionTime;
    }

    public ZonedDateTime getPleaDate() {
        return pleaDate;
    }

    public void setPleaDate(ZonedDateTime pleaDate) {
        this.pleaDate = pleaDate;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }
}
