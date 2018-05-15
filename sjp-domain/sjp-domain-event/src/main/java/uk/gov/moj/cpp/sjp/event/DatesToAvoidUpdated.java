package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("sjp.events.dates-to-avoid-updated")
public class DatesToAvoidUpdated {

    private final UUID caseId;
    private final String datesToAvoid;

    @JsonCreator
    public DatesToAvoidUpdated(final @JsonProperty("caseId") UUID caseId,
            final @JsonProperty("datesToAvoid") String datesToAvoid) {
        this.caseId = caseId;
        this.datesToAvoid = datesToAvoid;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }
}