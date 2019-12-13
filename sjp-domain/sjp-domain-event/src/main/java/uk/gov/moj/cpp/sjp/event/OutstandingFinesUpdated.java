package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(OutstandingFinesUpdated.EVENT_NAME)
public class OutstandingFinesUpdated {

    public static final String EVENT_NAME = "sjp.events.outstanding-fines-updated";

    private final UUID caseId;
    private final Boolean outstandingFines;
    private final ZonedDateTime updatedDate;

    @JsonCreator
    public OutstandingFinesUpdated(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("outstandingFines") final Boolean outstandingFines,
            @JsonProperty("updatedDate") final ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.outstandingFines = outstandingFines;
        this.updatedDate = updatedDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Boolean getOutstandingFines() {
        return outstandingFines;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }
}
