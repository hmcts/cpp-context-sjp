package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

@Event("sjp.events.plea-update-failed")
public class OffenceNotFound {

    private final String offenceId;
    private final String description;

    public OffenceNotFound(String offenceId, String description) {
        this.offenceId = offenceId;
        this.description = description;
    }

    public String getOffenceId() {
        return offenceId;
    }

    public String getDescription() {
        return description;
    }
}
