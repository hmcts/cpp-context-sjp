package uk.gov.moj.cpp.sjp.domain.resulting;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Offence {

    private final UUID id;
    private final List<Result> results;

    @JsonCreator
    public Offence(@JsonProperty("id") final UUID id,
                   @JsonProperty("results") final List<Result> results) {
        this.id = id;
        this.results = results;
    }

    public UUID getId() {
        return id;
    }

    public List<Result> getResults() {
        return results;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }

}
