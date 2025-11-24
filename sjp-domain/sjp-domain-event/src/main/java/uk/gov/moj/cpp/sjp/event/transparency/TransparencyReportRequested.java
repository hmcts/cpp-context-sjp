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
 * Event to indicate that the generation of a press transparency report has requested.
 * Use either the JSON or PDF report generation requested events instead.
 *
 * @deprecated
 */
@Deprecated(forRemoval = true)
@Event(TransparencyReportRequested.EVENT_NAME)
@SuppressWarnings("squid:S1133")
public class TransparencyReportRequested {
    public static final String EVENT_NAME = "sjp.events.transparency-report-requested";

    /**
     * Default value assigned for event replay where old versions of this
     * event don't have an id.
     */
    private UUID transparencyReportId = randomUUID();

    private ZonedDateTime requestedAt;

    @JsonCreator
    public TransparencyReportRequested(
            @JsonProperty("transparencyReportId") final UUID transparencyReportId,
            @JsonProperty("requestedAt") final ZonedDateTime requestedAt) {
        if (transparencyReportId != null) {
            this.transparencyReportId = transparencyReportId;
        }
        this.requestedAt = requestedAt;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    public UUID getTransparencyReportId() {
        return transparencyReportId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
