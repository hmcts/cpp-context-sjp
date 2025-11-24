package uk.gov.moj.cpp.sjp.domain.onlineplea;


import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityFinancialMeans;

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
    private final Boolean outstandingFines;
    private final PersonalDetails personalDetails;
    private final FinancialMeans financialMeans;
    private final Employer employer;
    private final List<Outgoing> outgoings;
    private final Boolean comeToCourt;
    private final DisabilityNeeds disabilityNeeds;

    private final LegalEntityDefendant legalEntityDefendant;

    private final LegalEntityFinancialMeans legalEntityFinancialMeans;


    @JsonCreator
    public PleadOnline(@JsonProperty("defendantId") final UUID defendantId,
                       @JsonProperty("offences") final List<Offence> offences,
                       @JsonProperty("unavailability") final String unavailability,
                       @JsonProperty("interpreterLanguage") final String interpreterLanguage,
                       @JsonProperty("speakWelsh") final Boolean speakWelsh,
                       @JsonProperty("witnessDetails") final String witnessDetails,
                       @JsonProperty("witnessDispute") final String witnessDispute,
                       @JsonProperty("outstandingFines") final Boolean outstandingFines,
                       @JsonProperty("personalDetails") final PersonalDetails personalDetails,
                       @JsonProperty("financialMeans") final FinancialMeans financialMeans,
                       @JsonProperty("employer") final Employer employer,
                       @JsonProperty("outgoings") final List<Outgoing> outgoings,
                       @JsonProperty("comeToCourt") final Boolean comeToCourt,
                       @JsonProperty("disabilityNeeds") final DisabilityNeeds disabilityNeeds,
                       @JsonProperty("legalEntityDefendant") final LegalEntityDefendant legalEntityDefendant,
                       @JsonProperty("legalEntityFinancialMeans") final LegalEntityFinancialMeans legalEntityFinancialMeans) {
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
        this.comeToCourt = comeToCourt;
        this.outstandingFines = outstandingFines;
        this.disabilityNeeds = disabilityNeeds;
        this.legalEntityDefendant = legalEntityDefendant;
        this.legalEntityFinancialMeans = legalEntityFinancialMeans;
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

    public Boolean getOutstandingFines() {
        return outstandingFines;
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

    public Boolean getComeToCourt() {
        return comeToCourt;
    }

    public DisabilityNeeds getDisabilityNeeds() {
        return disabilityNeeds;
    }

    public LegalEntityDefendant getLegalEntityDefendant() { return legalEntityDefendant;}

    public  LegalEntityFinancialMeans getLegalEntityFinancialMeans() { return legalEntityFinancialMeans; }

}
