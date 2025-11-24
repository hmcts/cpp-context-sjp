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

    private ReferencedDecisionsSaved(final Builder builder) {
        caseId = builder.caseId;
        sjpSessionId = builder.sjpSessionId;
        resultedOn = builder.resultedOn;
        verdict = builder.verdict;
        offences = builder.offences;
        accountDivisionCode = builder.accountDivisionCode;
        enforcingCourtCode = builder.enforcingCourtCode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(final ReferencedDecisionsSaved copy) {
        Builder builder = new Builder();
        builder.caseId = copy.getCaseId();
        builder.sjpSessionId = copy.getSjpSessionId();
        builder.resultedOn = copy.getResultedOn();
        builder.verdict = copy.getVerdict();
        builder.offences = copy.getOffences();
        builder.accountDivisionCode = copy.getAccountDivisionCode().orElse(null);
        builder.enforcingCourtCode = copy.getEnforcingCourtCode().orElse(null);
        return builder;
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


    public static final class Builder {
        private UUID caseId;
        private UUID sjpSessionId;
        private ZonedDateTime resultedOn;
        private String verdict;
        private List<Offence> offences;
        private Integer accountDivisionCode;
        private Integer enforcingCourtCode;

        private Builder() {
        }

        public Builder withCaseId(final UUID val) {
            caseId = val;
            return this;
        }

        public Builder withSjpSessionId(final UUID val) {
            sjpSessionId = val;
            return this;
        }

        public Builder withResultedOn(final ZonedDateTime val) {
            resultedOn = val;
            return this;
        }

        public Builder withVerdict(final String val) {
            verdict = val;
            return this;
        }

        public Builder withOffences(final List<Offence> val) {
            offences = val;
            return this;
        }

        public Builder withAccountDivisionCode(final Integer val) {
            accountDivisionCode = val;
            return this;
        }

        public Builder withEnforcingCourtCode(final Integer val) {
            enforcingCourtCode = val;
            return this;
        }

        public ReferencedDecisionsSaved build() {
            return new ReferencedDecisionsSaved(this);
        }
    }
}
