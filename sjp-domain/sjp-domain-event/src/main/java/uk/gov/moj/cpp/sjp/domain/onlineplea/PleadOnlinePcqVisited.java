package uk.gov.moj.cpp.sjp.domain.onlineplea;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PleadOnlinePcqVisited {

    private final UUID caseId;
    private final UUID defendantId;
    private final UUID pcqId;
    private final String urn;
    private final String type;

    @JsonCreator
    public PleadOnlinePcqVisited(@JsonProperty("defendantId") final UUID defendantId,
                                 @JsonProperty("caseId") final UUID caseId,
                                 @JsonProperty("urn") final String urn,
                                 @JsonProperty("type") final String type,
                                 @JsonProperty("pcqId") final UUID pcqId) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.pcqId = pcqId;
        this.urn = urn;
        this.type = type;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getUrn() {
        return urn;
    }

    public String getType() {
        return type;
    }

    public UUID getPcqId() {
        return pcqId;
    }
}
