package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@SuppressWarnings("squid:S2384")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AOCPCostDefendant implements Serializable {

    private static final long serialVersionUID = 7906175354400648983L;

    private final UUID id;

    private final List<AOCPCostOffence> offences;

    @JsonCreator
    public AOCPCostDefendant(@JsonProperty("id") final UUID id,
                             @JsonProperty("offences") final List<AOCPCostOffence> offences) {
        this.id = id;
        this.offences = Optional.ofNullable(offences).map(Collections::unmodifiableList).orElseGet(Collections::emptyList);
    }

    public UUID getId() {
        return id;
    }

    public List<AOCPCostOffence> getOffences() {
        return offences;
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
