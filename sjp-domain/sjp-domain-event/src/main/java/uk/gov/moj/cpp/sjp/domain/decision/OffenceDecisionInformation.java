package uk.gov.moj.cpp.sjp.domain.decision;

import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class OffenceDecisionInformation implements Serializable {

    private final UUID offenceId;
    private final VerdictType verdict;

    @JsonCreator
    public OffenceDecisionInformation(
            @JsonProperty("offenceId") final UUID offenceId,
            @JsonProperty("verdict") final VerdictType verdict) {
        this.offenceId = offenceId;
        this.verdict = verdict;
    }

    public static OffenceDecisionInformation createOffenceDecisionInformation(
            final UUID offenceId,
            final VerdictType verdict) {
        return new OffenceDecisionInformation(offenceId, verdict);
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public VerdictType getVerdict() {
        return verdict;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
