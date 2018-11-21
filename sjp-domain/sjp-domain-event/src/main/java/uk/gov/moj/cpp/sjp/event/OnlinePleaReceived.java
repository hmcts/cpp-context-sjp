package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.online-plea-received")
public class OnlinePleaReceived {

    private final String urn;
    private final UUID caseId;
    private final UUID defendantId;
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
    public OnlinePleaReceived(@JsonProperty("urn") final String urn,
                              @JsonProperty("caseId") final UUID caseId,
                              @JsonProperty("defendantId") final UUID defendantId,
                              @JsonProperty("unavailability") final String unavailability,
                              @JsonProperty("interpreterLanguage") final String interpreterLanguage,
                              @JsonProperty("speakWelsh") final Boolean speakWelsh,
                              @JsonProperty("witnessDetails") final String witnessDetails,
                              @JsonProperty("witnessDispute") final String witnessDispute,
                              @JsonProperty("personalDetails") final PersonalDetails personalDetails,
                              @JsonProperty("financialMeans") final FinancialMeans financialMeans,
                              @JsonProperty("employer") final Employer employer,
                              @JsonProperty("outgoings") final List<Outgoing> outgoings) {
        this.urn = urn;
        this.caseId = caseId;
        this.defendantId = defendantId;
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

    public String getUrn() {
        return urn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
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
