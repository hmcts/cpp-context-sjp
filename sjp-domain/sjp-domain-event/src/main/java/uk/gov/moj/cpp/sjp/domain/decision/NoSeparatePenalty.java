package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.NO_SEPARATE_PENALTY;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NoSeparatePenalty extends SingleOffenceDecision {

    private Boolean guiltyPleaTakenIntoAccount;
    private Boolean licenceEndorsed;
    private LocalDate convictionDate;

    public NoSeparatePenalty(@JsonProperty("id") final UUID id,
                             @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                             @JsonProperty("guiltyPleaTakenIntoAccount") final Boolean guiltyPleaTakenIntoAccount,
                             @JsonProperty("licenceEndorsed") final Boolean licenceEndorsed) {
        super(id, NO_SEPARATE_PENALTY, offenceDecisionInformation);
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.licenceEndorsed = licenceEndorsed;
    }

    public static NoSeparatePenalty createNoSeparatePenalty(final UUID id,
                                                            final OffenceDecisionInformation offenceDecisionInformation,
                                                            final Boolean guiltyPleaTakenIntoAccount,
                                                            final Boolean licenceEndorsement) {
        return new NoSeparatePenalty(id, offenceDecisionInformation, guiltyPleaTakenIntoAccount, licenceEndorsement);
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
