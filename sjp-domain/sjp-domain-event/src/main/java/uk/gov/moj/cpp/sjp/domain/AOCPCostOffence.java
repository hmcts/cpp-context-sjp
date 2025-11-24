package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AOCPCostOffence implements Serializable {
    private static final long serialVersionUID = 6519111656768457899L;

    private final UUID id;
    private final BigDecimal compensation;
    private final BigDecimal aocpStandardPenaltyAmount;
    private final Boolean isEligibleAOCP;
    private final Boolean prosecutorOfferAOCP;

    @JsonCreator
    public AOCPCostOffence(@JsonProperty("id") UUID id,
                           @JsonProperty("compensation") BigDecimal compensation,
                           @JsonProperty("aocpStandardPenaltyAmount") BigDecimal aocpStandardPenaltyAmount,
                           @JsonProperty("isEligibleAOCP") Boolean isEligibleAOCP,
                           @JsonProperty("prosecutorOfferAOCP") Boolean prosecutorOfferAOCP) {
        this.id = id;
        this.compensation = compensation;
        this.aocpStandardPenaltyAmount = aocpStandardPenaltyAmount;
        this.isEligibleAOCP = isEligibleAOCP;
        this.prosecutorOfferAOCP = prosecutorOfferAOCP;

    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public BigDecimal getAocpStandardPenaltyAmount() {
        return aocpStandardPenaltyAmount;
    }

    public Boolean getIsEligibleAOCP() {
        return isEligibleAOCP;
    }

    public Boolean getProsecutorOfferAOCP() {
        return prosecutorOfferAOCP;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
