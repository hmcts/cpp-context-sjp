package uk.gov.moj.cpp.sjp.event.transparency;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportGenerationFailed.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Event to indicate that the generation of a transparency report has failed.
 * Use either the JSON or PDF report generation failed events instead.
 *
 * @deprecated
 */
@Deprecated(forRemoval = true)
@Event(EVENT_NAME)
@SuppressWarnings("squid:S1133")
public class TransparencyReportGenerationFailed {

    public static final String EVENT_NAME = "sjp.events.transparency-report-generation-failed";

    private final UUID transparencyReportId;

    private final String templateIdentifier;

    private final List<UUID> caseIds;

    private final boolean reportGenerationPreviouslyFailed;

    @JsonCreator
    public TransparencyReportGenerationFailed(@JsonProperty("transparencyReportId") final UUID transparencyReportId,
                                              @JsonProperty("templateIdentifier") final String templateIdentifier,
                                              @JsonProperty("caseIds") final List<UUID> caseIds,
                                              @JsonProperty("reportGenerationPreviouslyFailed") final boolean reportGenerationPreviouslyFailed) {
        this.transparencyReportId = requireNonNull(transparencyReportId);
        this.templateIdentifier = requireNonNull(templateIdentifier);
        this.caseIds = requireNonNull(caseIds);
        this.reportGenerationPreviouslyFailed = reportGenerationPreviouslyFailed;
    }

    public UUID getTransparencyReportId() {
        return transparencyReportId;
    }

    public String getTemplateIdentifier() {
        return templateIdentifier;
    }

    public List<UUID> getCaseIds() {
        return unmodifiableList(caseIds);
    }

    public boolean isReportGenerationPreviouslyFailed() {
        return reportGenerationPreviouslyFailed;
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
