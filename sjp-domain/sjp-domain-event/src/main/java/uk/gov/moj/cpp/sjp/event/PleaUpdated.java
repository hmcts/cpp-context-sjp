package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;

import java.util.UUID;

@Event("sjp.events.plea-updated")
public class PleaUpdated {
    private String caseId;
    private String offenceId;
    private String plea;
    private PleaMethod pleaMethod;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Plea pleaDetails;

    public PleaUpdated() {
        //default constructor
    }

    public PleaUpdated(UUID caseId, Plea pleaDetails, PleaMethod pleaMethod) {
        this(caseId.toString(), pleaDetails.getOffenceId(), pleaDetails.getType().toString(), pleaMethod);
        this.pleaDetails = pleaDetails;
    }
    public PleaUpdated(String caseId, String offenceId, String plea, PleaMethod pleaMethod) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.plea = plea;
        this.pleaMethod = pleaMethod;
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

    public PleaMethod getPleaMethod() {
        return pleaMethod;
    }

    public Plea getPleaDetails() {
        return pleaDetails;
    }
}
