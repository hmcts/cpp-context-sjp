package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event for case reopened undone
 */
@Event("sjp.events.case-reopened-in-libra-undone")
public class CaseReopenedUndone implements Serializable {

    private static final long serialVersionUID = 4206671253527856605L;

    private final UUID caseId;

    private final LocalDate oldReopenedDate;

    @JsonCreator
    public CaseReopenedUndone(final @JsonProperty("caseId") UUID caseId, final @JsonProperty("oldReopenedDate") LocalDate oldReopenedDate) {
        this.caseId = caseId;
        this.oldReopenedDate = oldReopenedDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getOldReopenedDate() {
        return oldReopenedDate;
    }

}
