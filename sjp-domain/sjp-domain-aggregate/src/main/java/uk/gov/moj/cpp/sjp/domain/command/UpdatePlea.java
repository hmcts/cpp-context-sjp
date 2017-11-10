package uk.gov.moj.cpp.sjp.domain.command;


import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdatePlea extends ChangePlea {

    private final String plea;
    private final Boolean interpreterRequired;
    private final String interpreterLanguage;

    public UpdatePlea(UUID caseId,
                      UUID offenceId,
                      String plea) {
        this(caseId, offenceId, plea, null, null);
    }

    @JsonCreator
    public UpdatePlea(@JsonProperty("caseId") UUID caseId,
                      @JsonProperty("offenceId") UUID offenceId,
                      @JsonProperty("plea") String plea,
                      @JsonProperty("interpreterRequired") Boolean interpreterRequired,
                      @JsonProperty("interpreterLanguage") String interpreterLanguage) {
        super(caseId, offenceId);
        this.plea = plea;
        this.interpreterRequired = interpreterRequired;
        this.interpreterLanguage = interpreterLanguage;
    }

    public String getPlea() {
        return plea;
    }

    public Boolean getInterpreterRequired() {
        return interpreterRequired;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }
}
