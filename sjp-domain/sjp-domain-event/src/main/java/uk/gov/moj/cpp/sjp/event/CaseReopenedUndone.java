package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Event for case reopened undone
 */
@Event("sjp.events.case-reopened-in-libra-undone")
public class CaseReopenedUndone implements Serializable {

    private static final long serialVersionUID = 4206671253527856605L;

    private final UUID caseId;

    private final LocalDate oldReopenedDate;

    public CaseReopenedUndone(final UUID caseId, final LocalDate oldReopenedDate) {
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
