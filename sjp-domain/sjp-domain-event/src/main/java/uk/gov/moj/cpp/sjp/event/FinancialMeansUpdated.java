package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.Outgoing;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.financial-means-updated")
public class FinancialMeansUpdated {

    private UUID defendantId;
    private Income income;
    private Benefits benefits;
    private String employmentStatus;
    private final List<Outgoing> outgoings;

    @JsonCreator
    public FinancialMeansUpdated(@JsonProperty("defendantId") final UUID defendantId,
                                 @JsonProperty("income") final Income income,
                                 @JsonProperty("benefits") final Benefits benefits,
                                 @JsonProperty("employmentStatus") final String employmentStatus,
                                 @JsonProperty("outgoings")  List<Outgoing> outgoings) {
        this.defendantId = defendantId;
        this.income = income;
        this.benefits = benefits;
        this.employmentStatus = employmentStatus;
        this.outgoings = outgoings;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Income getIncome() {
        return income;
    }

    public Benefits getBenefits() {
        return benefits;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public List<Outgoing> getOutgoings() {
        return outgoings;
    }
}