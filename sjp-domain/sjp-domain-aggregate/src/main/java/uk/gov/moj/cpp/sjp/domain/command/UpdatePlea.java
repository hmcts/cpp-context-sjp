package uk.gov.moj.cpp.sjp.domain.command;


import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdatePlea extends ChangePlea {

    private final PleaType plea;
    private final Boolean interpreterRequired;
    private final String interpreterLanguage;

    public UpdatePlea(UUID caseId,
                      UUID offenceId,
                      PleaType plea) {
        this(caseId, offenceId, plea, null, null);
    }

    @JsonCreator
    public UpdatePlea(@JsonProperty("caseId") UUID caseId,
                      @JsonProperty("offenceId") UUID offenceId,
                      @JsonProperty("plea") PleaType plea,
                      @JsonProperty("interpreterRequired") Boolean interpreterRequired,
                      @JsonProperty("interpreterLanguage") String interpreterLanguage) {
        super(caseId, offenceId);
        this.plea = plea;
        this.interpreterRequired = interpreterRequired;
        this.interpreterLanguage = interpreterLanguage;
    }

    public PleaType getPlea() {
        return plea;
    }

    public Boolean getInterpreterRequired() {
        return interpreterRequired;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }
}
