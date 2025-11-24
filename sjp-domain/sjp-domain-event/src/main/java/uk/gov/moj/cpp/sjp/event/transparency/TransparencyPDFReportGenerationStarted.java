package uk.gov.moj.cpp.sjp.event.transparency;

import static java.util.Collections.unmodifiableList;

import uk.gov.justice.domain.annotation.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(TransparencyPDFReportGenerationStarted.EVENT_NAME)
public class TransparencyPDFReportGenerationStarted {

    public static final String EVENT_NAME = "sjp.events.transparency-pdf-report-generation-started";

    private UUID transparencyReportId;
    private final String format;
    private final String requestType;
    private final String title;
    private final String language;
    private final List<UUID> caseIds;

    @JsonCreator
    public TransparencyPDFReportGenerationStarted(
            @JsonProperty("transparencyReportId") final UUID transparencyReportId,
            @JsonProperty("format") final String format,
            @JsonProperty("requestType") final String requestType,
            @JsonProperty("title") final String title,
            @JsonProperty("language") final String language,
            @JsonProperty("caseIds") final List<UUID> caseIds) {
        this.transparencyReportId = transparencyReportId;
        this.format = format;
        this.requestType = requestType;
        this.title = title;
        this.language = language;
        this.caseIds = new LinkedList<>(caseIds);
    }

    public List<UUID> getCaseIds() {
        return unmodifiableList(caseIds);
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

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }
}
