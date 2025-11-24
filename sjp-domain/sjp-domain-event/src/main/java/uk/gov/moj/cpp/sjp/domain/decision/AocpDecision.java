package uk.gov.moj.cpp.sjp.domain.decision;

import uk.gov.justice.json.schemas.domains.sjp.User;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S00107", "PMD.BeanMembersShouldSerialize"})
public class AocpDecision implements Serializable {

    private UUID decisionId;
    private UUID sessionId;
    private UUID caseId;
    private User savedBy;
    private Defendant defendant;

    @JsonCreator
    public AocpDecision(@JsonProperty("decisionId") final UUID decisionId,
                        @JsonProperty("sessionId") final UUID sessionId,
                        @JsonProperty("caseId") final UUID caseId,
                        @JsonProperty("savedBy") final User savedBy,
                        @JsonProperty("defendant") final Defendant defendant) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.savedBy = savedBy;
        this.defendant = defendant;
    }


    public UUID getDecisionId() {
        return decisionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public User getSavedBy() {
        return savedBy;
    }

    public Defendant getDefendant() {
        return defendant;
    }

}
