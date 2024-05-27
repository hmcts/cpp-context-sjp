package uk.gov.moj.cpp.sjp.event.transparency;

import static java.util.Collections.unmodifiableList;

import uk.gov.justice.domain.annotation.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(TransparencyJSONReportGenerationStarted.EVENT_NAME)
public class TransparencyJSONReportGenerationStarted {

    public static final String EVENT_NAME = "sjp.events.transparency-json-report-generation-started";

    private UUID transparencyReportId;

    private final List<UUID> caseIds;

    @JsonCreator
    public TransparencyJSONReportGenerationStarted(
            @JsonProperty("transparencyReportId") final UUID transparencyReportId,
            @JsonProperty("caseIds") final List<UUID> caseIds) {
        this.transparencyReportId = transparencyReportId;
        this.caseIds = new LinkedList<>(caseIds);
    }

    public List<UUID> getCaseIds() {
        return unmodifiableList(caseIds);
    }

    public UUID getTransparencyReportId() {
        return transparencyReportId;
    }
}
