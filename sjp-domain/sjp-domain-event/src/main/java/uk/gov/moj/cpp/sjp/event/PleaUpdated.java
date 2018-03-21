package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;

import java.time.ZonedDateTime;

@Event("sjp.events.plea-updated")
public class PleaUpdated {
    private String caseId;
    private String offenceId;
    private String plea;
    private String mitigation;
    private String notGuiltyBecause;
    private PleaMethod pleaMethod;
    private ZonedDateTime updatedDate;

    public PleaUpdated() {
        //default constructor
    }

    public PleaUpdated(String caseId,
                       String offenceId,
                       String plea,
                       PleaMethod pleaMethod) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.plea = plea;
        this.pleaMethod = pleaMethod;
    }

    public PleaUpdated(String caseId,
                       String offenceId,
                       String plea,
                       String mitigation,
                       String notGuiltyBecause,
                       PleaMethod pleaMethod,
                       ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.plea = plea;
        this.mitigation = mitigation;
        this.notGuiltyBecause = notGuiltyBecause;
        this.pleaMethod = pleaMethod;
        this.updatedDate = updatedDate;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getOffenceId() {
        return offenceId;
    }

    public String getPlea() {
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
