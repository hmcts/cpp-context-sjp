package uk.gov.moj.cpp.sjp.event.transparency;


import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportGenerationFailed.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(EVENT_NAME)
public class PressTransparencyReportGenerationFailed {

    public static final String EVENT_NAME = "sjp.events.press-transparency-report-generation-failed";

    private final UUID pressTransparencyReportId;

    @JsonCreator
    public PressTransparencyReportGenerationFailed(@JsonProperty("pressTransparencyReportId") final UUID pressTransparencyReportId) {
        this.pressTransparencyReportId = pressTransparencyReportId;
    }

    public UUID getPressTransparencyReportId() {
        return pressTransparencyReportId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
