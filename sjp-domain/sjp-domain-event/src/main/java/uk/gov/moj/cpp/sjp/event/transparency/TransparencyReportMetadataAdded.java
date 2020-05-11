package uk.gov.moj.cpp.sjp.event.transparency;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(TransparencyReportMetadataAdded.EVENT_NAME)
public class TransparencyReportMetadataAdded {

    public static final String EVENT_NAME = "sjp.events.transparency-report-metadata-added";

    private final UUID transparencyReportId;
    private final ReportMetadata metadata;
    private final String language;

    @JsonCreator
    public TransparencyReportMetadataAdded(@JsonProperty("transparencyReportId") final UUID transparencyReportId,
                                           @JsonProperty("metadata") final ReportMetadata metadata,
                                           @JsonProperty("language") final String language) {
        this.transparencyReportId = transparencyReportId;
        this.metadata = metadata;
        this.language = language;
    }

    public UUID getTransparencyReportId() {
        return transparencyReportId;
    }

    public ReportMetadata getMetadata() {
        return metadata;
    }

    public String getLanguage() {
        return language;
    }
}
