package uk.gov.moj.cpp.sjp.domain.resulting;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class TerminalEntry {

    private final int index;
    private final String value;

    public TerminalEntry(final int index, final String value) {
        this.index = index;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TerminalEntry that = (TerminalEntry) o;
        return index == that.index &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, value);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}

