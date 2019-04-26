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

    private final String code;
    private final UUID resultTypeId;
    private final List<TerminalEntry> terminalEntries;

    @JsonCreator
    public Result(@JsonProperty("code") final String code,
                  @JsonProperty("resultTypeId") final UUID resultTypeId,
                  @JsonProperty("terminalEntries") final List<TerminalEntry> terminalEntries) {
        this.code = code;
        this.resultTypeId = resultTypeId;
        this.terminalEntries = ofNullable(terminalEntries).orElse(emptyList());
    }

    public String getCode() {
        return code;
    }

    public UUID getResultTypeId() {
        return resultTypeId;
    }

    public List<TerminalEntry> getTerminalEntries() {
        return terminalEntries;
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

