package uk.gov.moj.cpp.sjp.event.transparency;

import static java.util.UUID.randomUUID;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Event to indicate that the generation of a press transparency report has requested. Use either the JSON or PDF report generation requested events instead.
 *
 * @deprecated
 */
@Deprecated
@Event(PressTransparencyReportRequested.EVENT_NAME)
@SuppressWarnings("squid:S1133")
public class PressTransparencyReportRequested {
    public static final String EVENT_NAME = "sjp.events.press-transparency-report-requested";

    private UUID pressTransparencyReportId = randomUUID();
    private ZonedDateTime requestedAt;

    @JsonCreator
    public PressTransparencyReportRequested(
            @JsonProperty("pressTransparencyReportId") final UUID reportId,
            @JsonProperty("requestedAt") final ZonedDateTime requestedAt) {
        if (reportId != null) {
            this.pressTransparencyReportId = reportId;
        }
        this.requestedAt = requestedAt;
    }

    public UUID getPressTransparencyReportId() {
        return pressTransparencyReportId;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
