package uk.gov.moj.cpp.sjp.domain.resulting;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CaseResults {

    private UUID caseId;

    private Integer accountDivisionCode;

    private Integer enforcingCourtCode;

    private List<CaseDecision> caseDecisions;

    @JsonCreator
    public CaseResults(@JsonProperty("caseId") final UUID caseId,
                       @JsonProperty("accountDivisionCode") final Integer accountDivisionCode,
                       @JsonProperty("enforcingCourtCode") final Integer enforcingCourtCode,
                       @JsonProperty("caseDecisions") final List<CaseDecision> caseDecisions) {
        this.caseId = caseId;
        this.accountDivisionCode = accountDivisionCode;
        this.enforcingCourtCode = enforcingCourtCode;
        this.caseDecisions = caseDecisions;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Integer getAccountDivisionCode() {
        return accountDivisionCode;
    }

    public Integer getEnforcingCourtCode() {
        return enforcingCourtCode;
    }

    public List<CaseDecision> getCaseDecisions() {
        return caseDecisions;
    }
}
