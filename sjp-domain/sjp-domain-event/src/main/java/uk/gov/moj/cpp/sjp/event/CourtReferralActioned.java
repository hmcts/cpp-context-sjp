package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.court-referral-actioned")
public class CourtReferralActioned {

    private final UUID caseId;
    private final ZonedDateTime actioned;

    @JsonCreator
    public CourtReferralActioned(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("actioned") final ZonedDateTime actioned) {
        this.caseId = caseId;
        this.actioned = actioned;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getActioned() {
        return actioned;
    }
}
