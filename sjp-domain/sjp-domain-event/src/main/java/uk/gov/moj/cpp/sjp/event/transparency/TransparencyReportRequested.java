package uk.gov.moj.cpp.sjp.event.transparency;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(TransparencyReportRequested.EVENT_NAME)
public class TransparencyReportRequested {
    public static final String EVENT_NAME = "sjp.events.transparency-report-requested";

    private ZonedDateTime requestedAt;

    @JsonCreator
    public TransparencyReportRequested(@JsonProperty("requestedAt") final ZonedDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
