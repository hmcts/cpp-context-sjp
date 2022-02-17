package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.NO_SEPARATE_PENALTY;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NoSeparatePenalty extends SingleOffenceDecision implements ConvictingDecision {

    private Boolean guiltyPleaTakenIntoAccount;
    private Boolean licenceEndorsed;
    private LocalDate convictionDate;
    private SessionCourt convictingCourt;

    public NoSeparatePenalty(final UUID id,
                             final OffenceDecisionInformation offenceDecisionInformation,
                             final Boolean guiltyPleaTakenIntoAccount,
                             final Boolean licenceEndorsed) {
        this(id, offenceDecisionInformation, guiltyPleaTakenIntoAccount, licenceEndorsed, null);
    }

    @JsonCreator
    public NoSeparatePenalty(@JsonProperty("id") final UUID id,
                             @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                             @JsonProperty("guiltyPleaTakenIntoAccount") final Boolean guiltyPleaTakenIntoAccount,
                             @JsonProperty("licenceEndorsed") final Boolean licenceEndorsed,
                             @JsonProperty("pressRestriction") final PressRestriction pressRestriction) {
        super(id, NO_SEPARATE_PENALTY, offenceDecisionInformation, pressRestriction);
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.licenceEndorsed = licenceEndorsed;
    }

    public static NoSeparatePenalty createNoSeparatePenalty(final UUID id,
                                                            final OffenceDecisionInformation offenceDecisionInformation,
                                                            final Boolean guiltyPleaTakenIntoAccount,
                                                            final Boolean licenceEndorsement,
                                                            final PressRestriction pressRestriction) {
        return new NoSeparatePenalty(id, offenceDecisionInformation, guiltyPleaTakenIntoAccount,
                licenceEndorsement, pressRestriction);
    }

    public Boolean getGuiltyPleaTakenIntoAccount() {
        return guiltyPleaTakenIntoAccount;
    }

    public void setGuiltyPleaTakenIntoAccount(final Boolean guiltyPleaTakenIntoAccount) {
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
    }

    public Boolean getLicenceEndorsed() {
        return licenceEndorsed;
    }

    public void setLicenceEndorsed(final Boolean licenceEndorsed) {
        this.licenceEndorsed = licenceEndorsed;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    public SessionCourt getConvictingCourt() { return convictingCourt; }

    public void setConvictingCourt(final SessionCourt convictingCourt) {
        this.convictingCourt = convictingCourt;
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }
}
