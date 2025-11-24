package uk.gov.moj.cpp.sjp.event.transparency;

import static java.util.UUID.randomUUID;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.DocumentFormat;
import uk.gov.moj.cpp.sjp.domain.DocumentRequestType;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(PressTransparencyJSONReportRequested.EVENT_NAME)
public class PressTransparencyJSONReportRequested {
    public static final String EVENT_NAME = "sjp.events.press-transparency-json-report-requested";

    private UUID pressTransparencyReportId = randomUUID();
    private final String format = DocumentFormat.JSON.name();
    private String requestType = DocumentRequestType.DELTA.name();
    private String language;
    private ZonedDateTime requestedAt;

    @JsonCreator
    public PressTransparencyJSONReportRequested(
            @JsonProperty("pressTransparencyReportId") final UUID reportId,
            @JsonProperty("requestType") final String requestType,
            @JsonProperty("language") final String language,
            @JsonProperty("requestedAt") final ZonedDateTime requestedAt) {
        if (reportId != null) {
            this.pressTransparencyReportId = reportId;
            this.requestType = requestType;
            this.language = language;
        }
        this.requestedAt = requestedAt;
    }

    public UUID getPressTransparencyReportId() {
        return pressTransparencyReportId;
    }

    public String getFormat() {
        return format;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getLanguage() {
        return language;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
