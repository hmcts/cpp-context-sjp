package uk.gov.moj.cpp.sjp.event.transparency;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(PressTransparencyReportMetadataAdded.EVENT_NAME)
public class PressTransparencyReportMetadataAdded {

    public static final String EVENT_NAME = "sjp.events.press-transparency-report-metadata-added";

    private final UUID pressTransparencyReportId;
    private final ReportMetadata metadata;

    @JsonCreator
    public PressTransparencyReportMetadataAdded(@JsonProperty("pressTransparencyReportId") final UUID pressTransparencyReportId,
                                                @JsonProperty("metadata") final ReportMetadata metadata) {
        this.pressTransparencyReportId = pressTransparencyReportId;
        this.metadata = metadata;
    }

    public UUID getPressTransparencyReportId() {
        return pressTransparencyReportId;
    }

    public ReportMetadata getMetadata() {
        return metadata;
    }

}
