package uk.gov.moj.cpp.sjp.domain.onlineplea;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S2384", "squid:CallToDeprecatedMethod"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class PleadAocpOnline {

    private final UUID caseId;
    private final UUID defendantId;
    private final List<Offence> offences;
    private final boolean aocpAccepted;
    private final PersonalDetails personalDetails;

    @JsonCreator
    public PleadAocpOnline(@JsonProperty("caseId") final UUID caseId,
                           @JsonProperty("defendantId") final UUID defendantId,
                           @JsonProperty("offences") final List<Offence> offences,
                           @JsonProperty("aocpAccepted") final boolean aocpAccepted,
                           @JsonProperty("personalDetails") final PersonalDetails personalDetails) {

        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offences = offences;
        this.aocpAccepted = aocpAccepted;
        this.personalDetails = personalDetails;

    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public boolean getAocpAccepted() {
        return aocpAccepted;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }

}
