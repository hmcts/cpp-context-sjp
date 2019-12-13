package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ADJOURN")
public class AdjournOffenceDecision extends OffenceDecision {

    @Column(name = "adjournment_reason")
    private String adjournmentReason;

    @Column(name = "adjourned_to")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate adjournedTo;

    public AdjournOffenceDecision() {
        super();
    }

    public AdjournOffenceDecision(final UUID offenceId, final UUID caseDecisionId,
                                  final String adjournmentReason,
                                  final LocalDate adjournedTo,
                                  final VerdictType verdict) {

        super(offenceId, caseDecisionId, DecisionType.ADJOURN, verdict);
        this.adjournmentReason = adjournmentReason;
        this.adjournedTo = adjournedTo;
    }

    public String getAdjournmentReason() {
        return adjournmentReason;
    }

    public LocalDate getAdjournedTo() {
        return adjournedTo;
    }
}
