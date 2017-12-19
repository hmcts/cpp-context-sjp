package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.trial-requested")
public class TrialRequested {
    private final UUID caseId;
    private final String unavailability;
    private final String witnessDetails;
    private final String witnessDispute;

    @JsonCreator
    public TrialRequested(final UUID caseId, final String unavailability, final String witnessDetails, final String witnessDispute) {
        this.caseId = caseId;
        this.unavailability = unavailability;
        this.witnessDetails = witnessDetails;
        this.witnessDispute = witnessDispute;
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
}
