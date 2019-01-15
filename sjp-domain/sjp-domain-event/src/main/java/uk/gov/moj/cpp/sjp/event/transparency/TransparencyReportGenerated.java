package uk.gov.moj.cpp.sjp.event.transparency;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(TransparencyReportGenerated.EVENT_NAME)
public class TransparencyReportGenerated {

    public static final String EVENT_NAME = "sjp.events.transparency-report-generated";

    private final List<UUID> caseIds;
    private final ReportMetadata englishReportMetadata;
    private final ReportMetadata welshReportMetadata;

    @JsonCreator
    public TransparencyReportGenerated(@JsonProperty("caseIds") final List<UUID> caseIds,
                                       @JsonProperty("englishReportMetadata") final ReportMetadata englishReportMetadata,
                                       @JsonProperty("welshReportMetadata") final ReportMetadata welshReportMetadata) {
        this.caseIds = caseIds;
        this.englishReportMetadata = englishReportMetadata;
        this.welshReportMetadata = welshReportMetadata;
    }

    public List<UUID> getCaseIds() {
        return caseIds;
    }


    public ReportMetadata getEnglishReportMetadata() {
        return englishReportMetadata;
    }

    public ReportMetadata getWelshReportMetadata() {
        return welshReportMetadata;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
