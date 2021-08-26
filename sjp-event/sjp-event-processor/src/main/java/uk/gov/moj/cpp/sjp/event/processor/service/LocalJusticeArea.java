package uk.gov.moj.cpp.sjp.event.processor.service;

import javax.json.JsonObject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class LocalJusticeArea {
    private final String nationalCourtCode;
    private final String name;

    public LocalJusticeArea(final String nationalCourtCode, final String name) {
        this.nationalCourtCode = nationalCourtCode;
        this.name = name;
    }

    public static LocalJusticeArea fromJson(final JsonObject localJusticeArea) {
        return new LocalJusticeArea(localJusticeArea.getString("nationalCourtCode"),
                localJusticeArea.getString("name"));
    }

    public String getNationalCourtCode() {
        return nationalCourtCode;
    }

    public String getName() {
        return name;
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
        return ToStringBuilder.reflectionToString(this);
    }
}
