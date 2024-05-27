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

@Event(TransparencyPDFReportRequested.EVENT_NAME)
public class TransparencyPDFReportRequested {
    public static final String EVENT_NAME = "sjp.events.transparency-pdf-report-requested";

    /**
     * Default value assigned for event replay where old versions of this
     * event don't have an id.
     */
    private UUID transparencyReportId = randomUUID();
    private final String format = DocumentFormat.PDF.name();
    private String requestType = DocumentRequestType.DELTA.name();
    private String language;
    private ZonedDateTime requestedAt;

    @JsonCreator
    public TransparencyPDFReportRequested(
            @JsonProperty("transparencyReportId") final UUID transparencyReportId,
            @JsonProperty("requestType") final String requestType,
            @JsonProperty("language") final String language,
            @JsonProperty("requestedAt") final ZonedDateTime requestedAt) {
        if (transparencyReportId != null) {
            this.transparencyReportId = transparencyReportId;
            this.requestType = requestType;
            this.language = language;
        }
        this.requestedAt = requestedAt;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    public UUID getTransparencyReportId() {
        return transparencyReportId;
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
