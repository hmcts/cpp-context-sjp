package uk.gov.moj.cpp.sjp.domain.resulting;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferencedDecisionsSaved {

    private final UUID caseId;
    private final UUID sjpSessionId;
    private final ZonedDateTime resultedOn;
    private final String verdict;
    private final List<Offence> offences;
    private final Integer accountDivisionCode;
    private final Integer enforcingCourtCode;

    @JsonCreator
    public ReferencedDecisionsSaved(@JsonProperty("caseId") final UUID caseId,
                                    @JsonProperty("sjpSessionId") final UUID sjpSessionId,
                                    @JsonProperty("resultedOn") final ZonedDateTime resultedOn,
                                    @JsonProperty("verdict") final String verdict,
                                    @JsonProperty("offences") final List<Offence> offences,
                                    @JsonProperty("accountDivisionCode") final Integer accountDivisionCode,
                                    @JsonProperty("enforcingCourtCode") final Integer enforcingCourtCode) {
        this.caseId = caseId;
        this.sjpSessionId = sjpSessionId;
        this.resultedOn = resultedOn;
        this.verdict = verdict;
        this.offences = offences;
        this.accountDivisionCode = accountDivisionCode;
        this.enforcingCourtCode = enforcingCourtCode;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getSjpSessionId() {
        return sjpSessionId;
    }

    public ZonedDateTime getResultedOn() {
        return resultedOn;
    }

    public String getVerdict() {
        return verdict;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public Optional<Integer> getAccountDivisionCode() {
        return Optional.ofNullable(accountDivisionCode);
    }

    public Optional<Integer> getEnforcingCourtCode() {
        return Optional.ofNullable(enforcingCourtCode);
    }


}
