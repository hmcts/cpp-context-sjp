package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event for case listed in criminal courts
 */
@Event(CaseListedInCriminalCourtsUpdated.EVENT_NAME)
public class CaseListedInCriminalCourtsUpdated {

    public static final String EVENT_NAME = "sjp.events.case-listed-in-criminal-courts-updated";

    private final UUID caseId;
    private final boolean listedInCriminalCourts;

    @JsonCreator
    public CaseListedInCriminalCourtsUpdated(@JsonProperty("caseId") final UUID caseId,
                                             @JsonProperty("listedInCriminalCourts") final boolean listedInCriminalCourts) {
        this.caseId = caseId;
        this.listedInCriminalCourts = listedInCriminalCourts;
    }

    public UUID getCaseId() {
        return caseId;
    }
    public boolean isListedInCriminalCourts(){ return listedInCriminalCourts; }

}
