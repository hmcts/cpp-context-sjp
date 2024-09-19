package uk.gov.moj.cpp.sjp.event.transparency;

import static java.util.Collections.unmodifiableList;

import uk.gov.justice.domain.annotation.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event to indicate that the generation of a press transparency report has started. Use either the JSON or PDF report generation started events instead.
 *
 * @deprecated
 */
@Deprecated(forRemoval = true)
@Event(PressTransparencyReportGenerationStarted.EVENT_NAME)
@SuppressWarnings("squid:S1133")
public class PressTransparencyReportGenerationStarted {

    public static final String EVENT_NAME = "sjp.events.press-transparency-report-generation-started";
    private UUID pressTransparencyReportId;
    private final List<UUID> caseIds;

    @JsonCreator
    public PressTransparencyReportGenerationStarted(
            @JsonProperty("pressTransparencyReportId") final UUID pressTransparencyReportId,
            @JsonProperty("caseIds") final List<UUID> caseIds) {
        this.pressTransparencyReportId = pressTransparencyReportId;
        this.caseIds = new LinkedList<>(caseIds);
    }

    public UUID getPressTransparencyReportId() {
        return pressTransparencyReportId;
    }

    public List<UUID> getCaseIds() {
        return unmodifiableList(caseIds);
    }

}
