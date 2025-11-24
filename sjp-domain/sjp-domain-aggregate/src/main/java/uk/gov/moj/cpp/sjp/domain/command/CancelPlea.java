package uk.gov.moj.cpp.sjp.domain.command;


import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CancelPlea extends ChangePlea {

    @JsonCreator
    public CancelPlea(@JsonProperty("caseId") UUID caseId,
                      @JsonProperty("offenceId") UUID offenceId) {
        super(caseId, offenceId);
    }

}
