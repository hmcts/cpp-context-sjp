package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event for case listed in criminal courts
 */
@Event(CaseListedInCriminalCourts.EVENT_NAME)
public class CaseListedInCriminalCourts {

    public static final String EVENT_NAME = "sjp.events.case-listed-in-criminal-courts";

    private final UUID caseId;
    private final String hearingCourtName;
    private final ZonedDateTime hearingTime;

    @JsonCreator
    public CaseListedInCriminalCourts(@JsonProperty("caseId") final UUID caseId,
                                      @JsonProperty("hearingCourtName") final String hearingCourtName,
                                      @JsonProperty("hearingTime") final ZonedDateTime hearingTime) {
        this.caseId = caseId;
        this.hearingCourtName = hearingCourtName;
        this.hearingTime = hearingTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getHearingCourtName() {
        return hearingCourtName;
    }

    public ZonedDateTime getHearingTime() {
        return hearingTime;
    }

}
