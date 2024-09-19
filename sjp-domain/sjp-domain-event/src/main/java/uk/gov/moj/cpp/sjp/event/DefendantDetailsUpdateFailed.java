package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.defendant-update-failed")
public class DefendantDetailsUpdateFailed {

    private final UUID caseId;
    private final UUID defendantId;
    private final String description;

    @JsonCreator
    public DefendantDetailsUpdateFailed(final UUID caseId, final UUID defendantId, final String description) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.description = description;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }

}
