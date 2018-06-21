package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event(PleaUpdated.EVENT_NAME)
public class PleaUpdated {

    public static final String EVENT_NAME = "sjp.events.plea-updated";

    private final UUID caseId;
    private final UUID offenceId;
    private final PleaType plea;
    private final String mitigation;
    private final String notGuiltyBecause;
    private final PleaMethod pleaMethod;
    private final ZonedDateTime updatedDate;

    public PleaUpdated() {
        //default constructor
        this(null, null, null, null);
    }

    public PleaUpdated(final UUID caseId,
                       final UUID offenceId,
                       final PleaType plea,
                       final PleaMethod pleaMethod) {
        this(caseId, offenceId, plea, null, null, pleaMethod, null);
    }

    public PleaUpdated(final UUID caseId,
                       final UUID offenceId,
                       final PleaType plea,
                       final String mitigation,
                       final String notGuiltyBecause,
                       final PleaMethod pleaMethod,
                       final ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.plea = plea;
        this.pleaMethod = pleaMethod;
        this.mitigation = mitigation;
        this.notGuiltyBecause = notGuiltyBecause;
        this.updatedDate = updatedDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public PleaType getPlea() {
        return plea;
    }

    public String getMitigation() {
        return mitigation;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

    public PleaMethod getPleaMethod() {
        return pleaMethod;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

}
