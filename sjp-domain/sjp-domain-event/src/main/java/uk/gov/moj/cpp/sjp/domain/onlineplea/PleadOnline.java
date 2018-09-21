package uk.gov.moj.cpp.sjp.domain.onlineplea;

import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Outgoing;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PleadOnline {

    private final UUID defendantId;
    private final List<Offence> offences;
    private final String unavailability;
    private final String interpreterLanguage;
    private final Boolean speakWelsh;
    private final String witnessDetails;
    private final String witnessDispute;
    private final PersonalDetails personalDetails;
    private final FinancialMeans financialMeans;
    private final Employer employer;
    private final List<Outgoing> outgoings;

    @JsonCreator
    public PleadOnline(@JsonProperty("defendantId") final UUID defendantId,
                       @JsonProperty("offences") final List<Offence> offences,
                       @JsonProperty("unavailability") final String unavailability,
                       @JsonProperty("interpreterLanguage") final String interpreterLanguage,
                       @JsonProperty("speakWelsh") final Boolean speakWelsh,
                       @JsonProperty("witnessDetails") final String witnessDetails,
                       @JsonProperty("witnessDispute") final String witnessDispute,
                       @JsonProperty("personalDetails") final PersonalDetails personalDetails,
                       @JsonProperty("financialMeans") final FinancialMeans financialMeans,
                       @JsonProperty("employer") final Employer employer,
                       @JsonProperty("outgoings") final List<Outgoing> outgoings) {
        this.defendantId = defendantId;
        this.offences = offences;
        this.unavailability = unavailability;
        this.interpreterLanguage = interpreterLanguage;
        this.speakWelsh = speakWelsh;
        this.witnessDetails = witnessDetails;
        this.witnessDispute = witnessDispute;
        this.personalDetails = personalDetails;
        this.financialMeans = financialMeans;
        this.employer = employer;
        this.outgoings = outgoings;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public String getUnavailability() {
        return unavailability;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }

    public Boolean getSpeakWelsh() {
        return speakWelsh;
    }

    public String getWitnessDetails() {
        return witnessDetails;
    }

    public String getWitnessDispute() {
        return witnessDispute;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public FinancialMeans getFinancialMeans() {
        return financialMeans;
    }

    public Employer getEmployer() {
        return employer;
    }

    public List<Outgoing> getOutgoings() {
        return outgoings;
    }

}
