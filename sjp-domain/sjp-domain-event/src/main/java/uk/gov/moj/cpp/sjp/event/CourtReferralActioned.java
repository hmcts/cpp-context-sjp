package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event("sjp.events.court-referral-actioned")
public class CourtReferralActioned {

    private final UUID caseId;
    private final ZonedDateTime actioned;

    public CourtReferralActioned(final UUID caseId, final ZonedDateTime actioned) {
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
