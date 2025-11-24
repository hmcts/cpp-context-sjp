package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotification.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class PartialAocpCriteriaNotification {

    public static final String EVENT_NAME = "sjp.events.aocp-criteria-matched-partially";

    private final UUID caseId;
    private final String urn;
    private final String prosecutingAuthority;

    @JsonCreator
    public PartialAocpCriteriaNotification(@JsonProperty("caseId") UUID caseId,
                                           @JsonProperty("urn") String urn,
                                           @JsonProperty("prosecutingAuthority") String prosecutingAuthority) {
        this.caseId = caseId;
        this.urn = urn;
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() { return urn; }

    public String getProsecutingAuthority() { return prosecutingAuthority; }
}
