package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.trial-request-cancelled")
public class TrialRequestCancelled {
    private final UUID caseId;
    private ZonedDateTime cancelledDate;

    @JsonCreator
    public TrialRequestCancelled(@JsonProperty(value = "caseId") final UUID caseId,
                                 @JsonProperty(value = "updatedDate") final ZonedDateTime cancelledDate) {
        this.caseId = caseId;
        this.cancelledDate = cancelledDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getCancelledDate() {
        return cancelledDate;
    }
}
