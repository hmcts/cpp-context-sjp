package uk.gov.moj.cpp.sjp.event.transparency;

import static java.util.Collections.unmodifiableList;

import uk.gov.justice.domain.annotation.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(PressTransparencyPDFReportGenerationStarted.EVENT_NAME)
public class PressTransparencyPDFReportGenerationStarted {

    public static final String EVENT_NAME = "sjp.events.press-transparency-pdf-report-generation-started";

    private final UUID pressTransparencyReportId;
    private final String format;
    private final String requestType;
    private final String title;
    private final String language;
    private final List<UUID> caseIds;


    @JsonCreator
    public PressTransparencyPDFReportGenerationStarted(
            @JsonProperty("pressTransparencyReportId") final UUID pressTransparencyReportId,
            @JsonProperty("format") final String format,
            @JsonProperty("requestType") final String requestType,
            @JsonProperty("title") final String title,
            @JsonProperty("language") final String language,
            @JsonProperty("caseIds") final List<UUID> caseIds) {
        this.pressTransparencyReportId = pressTransparencyReportId;
        this.format = format;
        this.requestType = requestType;
        this.title = title;
        this.language = language;
        this.caseIds = new LinkedList<>(caseIds);
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

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    public List<UUID> getCaseIds() {
        return unmodifiableList(caseIds);
    }

}
