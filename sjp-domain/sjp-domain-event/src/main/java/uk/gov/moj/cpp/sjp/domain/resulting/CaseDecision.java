package uk.gov.moj.cpp.sjp.domain.resulting;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CaseDecision {

    private final UUID sjpSessionId;
    private final ZonedDateTime resultedOn;
    private final List<Offence> offences;

    @JsonCreator
    public CaseDecision(@JsonProperty("caseId") final UUID sjpSessionId,
                        @JsonProperty("resultedOn") final ZonedDateTime resultedOn,
                        @JsonProperty("offences") final List<Offence> offences) {
        this.sjpSessionId = sjpSessionId;
        this.resultedOn = resultedOn;
        this.offences = offences;
    }

    public UUID getSjpSessionId() {
        return sjpSessionId;
    }

    public ZonedDateTime getResultedOn() {
        return resultedOn;
    }

    public List<Offence> getOffences() {
        return offences;
    }

}
