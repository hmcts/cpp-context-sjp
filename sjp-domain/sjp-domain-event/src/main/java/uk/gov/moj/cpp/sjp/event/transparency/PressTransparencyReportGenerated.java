package uk.gov.moj.cpp.sjp.event.transparency;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Old event used to signal the generation of the press transparency report when
 * synchronous generation flow was used.
 * @deprecated
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Event(PressTransparencyReportGenerated.EVENT_NAME)
public class PressTransparencyReportGenerated {

    public static final String EVENT_NAME = "sjp.events.press-transparency-report-generated";

    private final List<UUID> caseIds;
    private final ReportMetadata reportMetadata;

    @JsonCreator
    public PressTransparencyReportGenerated(@JsonProperty("caseIds") final List<UUID> caseIds,
                                            @JsonProperty("reportMetadata") final ReportMetadata reportMetadata) {
        this.caseIds = caseIds;
        this.reportMetadata = reportMetadata;
    }

    public List<UUID> getCaseIds() {
        return caseIds;
    }

    public ReportMetadata getReportMetadata() {
        return reportMetadata;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
