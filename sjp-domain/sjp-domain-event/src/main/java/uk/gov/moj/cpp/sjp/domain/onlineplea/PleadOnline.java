package uk.gov.moj.cpp.sjp.domain.onlineplea;

import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Outgoing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PleadOnline {
    private String defendantId;
    private List<Offence> offences;
    private String unavailability;
    private String interpreterLanguage;
    private String witnessDetails;
    private String witnessDispute;
    private FinancialMeans financialMeans;
    private Employer employer;
    private List<Outgoing> outgoings;

    @JsonCreator
    public PleadOnline(@JsonProperty(value = "defendantId") final String defendantId,
                       @JsonProperty(value = "offences") final List<Offence> offences,
                       @JsonProperty(value = "unavailability") final String unavailability,
                       @JsonProperty(value = "interpreterLanguage") final String interpreterLanguage,
                       @JsonProperty(value = "witnessDetails") final String witnessDetails,
                       @JsonProperty(value = "witnessDispute") final String witnessDispute,
                       @JsonProperty(value = "financialMeans") final FinancialMeans financialMeans,
                       @JsonProperty(value = "employer") final Employer employer,
                       @JsonProperty(value = "outgoings") final List<Outgoing> outgoings) {
        this.defendantId = defendantId;
        this.offences = offences;
        this.unavailability = unavailability;
        this.interpreterLanguage = interpreterLanguage;
        this.witnessDetails = witnessDetails;
        this.witnessDispute = witnessDispute;
        this.financialMeans = financialMeans;
        this.employer = employer;
        this.outgoings = outgoings;
    }

    public String getDefendantId() {
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

    public String getWitnessDetails() {
        return witnessDetails;
    }

    public String getWitnessDispute() {
        return witnessDispute;
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
