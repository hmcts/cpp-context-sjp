package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
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

    @Override
    @SuppressWarnings({"squid:S1067", "squid:S00122"})
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TrialRequested that = (TrialRequested) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(unavailability, that.unavailability) &&
                Objects.equals(witnessDetails, that.witnessDetails) &&
                Objects.equals(witnessDispute, that.witnessDispute) &&
                Objects.equals(updatedDate, that.updatedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, unavailability, witnessDetails, witnessDispute, updatedDate);
    }
}
