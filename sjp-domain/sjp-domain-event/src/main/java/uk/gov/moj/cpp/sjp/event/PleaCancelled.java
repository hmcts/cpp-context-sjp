package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("structure.events.plea-cancelled")
public class PleaCancelled {
    private String caseId;
    private String offenceId;

    @JsonCreator
    public PleaCancelled(
            @JsonProperty("caseId") String caseId,
            @JsonProperty("offenceId") String offenceId) {
        this.caseId = caseId;
        this.offenceId = offenceId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getOffenceId() {
        return offenceId;
    }
}
