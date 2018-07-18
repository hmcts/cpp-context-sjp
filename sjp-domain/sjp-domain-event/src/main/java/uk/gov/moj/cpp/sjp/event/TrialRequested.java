package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.trial-requested")
public class TrialRequested {
    private final UUID caseId;
    private final String unavailability;
    private final String witnessDetails;
    private final String witnessDispute;
    private final ZonedDateTime updatedDate;

    @JsonCreator
    public TrialRequested(@JsonProperty("caseId") final UUID caseId,
                          @JsonProperty("unavailability") final String unavailability,
                          @JsonProperty("witnessDetails") final String witnessDetails,
                          @JsonProperty("witnessDispute") final String witnessDispute,
                          @JsonProperty("updatedDate") final ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.unavailability = unavailability;
        this.witnessDetails = witnessDetails;
        this.witnessDispute = witnessDispute;
        this.updatedDate = updatedDate;
    }

    public TrialRequested(final UUID caseId, final ZonedDateTime updatedDate) {
        this(caseId, null, null, null, updatedDate);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUnavailability() {
        return unavailability;
    }

    public String getWitnessDetails() {
        return witnessDetails;
    }

    public String getWitnessDispute() {
        return witnessDispute;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }
}
