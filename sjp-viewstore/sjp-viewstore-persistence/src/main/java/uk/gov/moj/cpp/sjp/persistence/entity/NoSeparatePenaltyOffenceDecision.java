package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(DecisionType.DecisionName.NO_SEPARATE_PENALTY)
public class NoSeparatePenaltyOffenceDecision extends OffenceDecision {

    @Column(name = "guilty_plea_taken_into_account")
    private Boolean guiltyPleaTakenIntoAccount;

    @Column(name = "licence_endorsement")
    private Boolean licenceEndorsement;

    public NoSeparatePenaltyOffenceDecision() {
        super();
    }

    public NoSeparatePenaltyOffenceDecision(final UUID offenceId,
                                            final UUID caseDecisionId,
                                            final VerdictType verdict,
                                            final LocalDate convictionDate,
                                            final Boolean guiltyPleaTakenIntoAccount,
                                            final Boolean licenceEndorsement, final PressRestriction pressRestriction) {

        super(offenceId, caseDecisionId, DecisionType.NO_SEPARATE_PENALTY, verdict, convictionDate, pressRestriction);
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.licenceEndorsement = licenceEndorsement;
    }

    public Boolean getGuiltyPleaTakenIntoAccount() {
        return guiltyPleaTakenIntoAccount;
    }

    public void setGuiltyPleaTakenIntoAccount(final Boolean guiltyPleaTakenIntoAccount) {
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
    }

    public Boolean getLicenceEndorsement() {
        return licenceEndorsement;
    }

    public void setLicenceEndorsement(final Boolean licenceEndorsement) {
        this.licenceEndorsement = licenceEndorsement;
    }
}
