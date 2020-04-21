package uk.gov.moj.cpp.sjp.domain.decision;

import static java.util.Arrays.asList;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class OffenceDecisionInformation implements Serializable {

    private static final List<VerdictType> CONVICTION_VERDICTS = asList(FOUND_GUILTY, PROVED_SJP);

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

    /**
     * Indicates if it is a conviction verdict.
     * @return boolean indicating if it's a conviction verdict
     */
    @JsonIgnore
    public boolean isConviction() {
        return this.verdict !=null && CONVICTION_VERDICTS.contains(this.verdict);
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
