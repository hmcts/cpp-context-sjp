package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event(MarkedAsLegalSocChecked.EVENT_NAME)
public class MarkedAsLegalSocChecked {

    public static final String EVENT_NAME = "sjp.events.marked-as-legal-soc-checked";

    private UUID caseId;

    private UUID checkedBy;

    private ZonedDateTime checkedAt;

    public MarkedAsLegalSocChecked(final UUID caseId, final UUID checkedBy, final ZonedDateTime checkedAt) {
        this.caseId = caseId;
        this.checkedBy = checkedBy;
        this.checkedAt = checkedAt;
    }

    public UUID getCheckedBy() {
        return checkedBy;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getCheckedAt() { return checkedAt; }
}
