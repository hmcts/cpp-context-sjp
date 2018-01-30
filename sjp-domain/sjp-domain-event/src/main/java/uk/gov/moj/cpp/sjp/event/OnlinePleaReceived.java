package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.online-plea-received")
public class OnlinePleaReceived {
    private String urn;
    private String caseId;
    private String defendantId;
    private String unavailability;
    private String interpreterLanguage;
    private String witnessDetails;
    private String witnessDispute;
    private PersonalDetails personalDetails;
    private FinancialMeans financialMeans;
    private Employer employer;
    private List<Outgoing> outgoings;

    @JsonCreator
    public OnlinePleaReceived(@JsonProperty(value = "urn") final String urn,
                              @JsonProperty(value = "caseId") final String caseId,
                              @JsonProperty(value = "defendantId") final String defendantId,
                              @JsonProperty(value = "unavailability") final String unavailability,
                              @JsonProperty(value = "interpreterLanguage") final String interpreterLanguage,
                              @JsonProperty(value = "witnessDetails") final String witnessDetails,
                              @JsonProperty(value = "witnessDispute") final String witnessDispute,
                              @JsonProperty(value = "personalDetails") final PersonalDetails personalDetails,
                              @JsonProperty(value = "financialMeans") final FinancialMeans financialMeans,
                              @JsonProperty(value = "employer") final Employer employer,
                              @JsonProperty(value = "outgoings") final List<Outgoing> outgoings) {
        this.urn = urn;
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.unavailability = unavailability;
        this.interpreterLanguage = interpreterLanguage;
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

    public String getCaseId() {
        return caseId;
    }

    public String getDefendantId() {
        return defendantId;
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
