package uk.gov.moj.cpp.sjp.domain.decision;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PressRestriction implements Serializable {

    private static final long serialVersionUID = -3920281790503401211L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("requested")
    private Boolean requested;

    @JsonCreator
    public PressRestriction(final String name) {
        this.name = name;
        this.requested = Boolean.TRUE;
    }

    public PressRestriction() {
        this.name = null;
        this.requested = false;
    }

    public static PressRestriction requested(final String name) {
        return new PressRestriction(name);
    }

    public static PressRestriction revoked() {
        return new PressRestriction();
    }

    public String getName() {
        return name;
    }

    public Boolean getRequested() {
        return requested;
    }

    @JsonIgnore
    public boolean isRevoked() {
        return isNull(name) && !this.requested;
    }

    @Override
    public boolean equals(final Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
