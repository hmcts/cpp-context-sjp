package uk.gov.moj.cpp.sjp.domain.resulting;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Result {

    private final UUID id;
    private final List<Prompt> prompts;

    @JsonCreator
    public Result(@JsonProperty("id") final UUID id,
                  @JsonProperty("prompts") final List<Prompt> prompts) {
        this.id = id;
        this.prompts = ofNullable(prompts).orElse(emptyList());
    }

    public UUID getId() {
        return id;
    }

    public List<Prompt> getPrompts() {
        return prompts;
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
