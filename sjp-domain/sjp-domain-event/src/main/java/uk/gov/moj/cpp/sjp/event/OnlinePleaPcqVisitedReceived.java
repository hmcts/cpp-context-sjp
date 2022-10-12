package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.online-plea-pcq-visited-received")
public class OnlinePleaPcqVisitedReceived {

    private final String urn;
    private final UUID caseId;
    private final UUID defendantId;
    private final UUID pcqId;

    @JsonCreator
    public OnlinePleaPcqVisitedReceived(@JsonProperty("caseId") final UUID caseId,
                                        @JsonProperty("urn") final String urn,
                                        @JsonProperty("defendantId") final UUID defendantId,
                                        @JsonProperty("pcqId") final UUID pcqId) {
        this.urn = urn;
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.pcqId = pcqId;
    }

    public String getUrn() {
        return urn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getPcqId() {
        return pcqId;
    }
}
