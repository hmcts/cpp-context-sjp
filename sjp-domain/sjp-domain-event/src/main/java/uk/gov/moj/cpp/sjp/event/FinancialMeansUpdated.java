package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("structure.events.financial-means-updated")
public class FinancialMeansUpdated {

    private UUID defendantId;
    private Income income;
    private Benefits benefits;
    private String employmentStatus;

    @JsonCreator
    public FinancialMeansUpdated(@JsonProperty("defendantId") final UUID defendantId,
                                 @JsonProperty("income") final Income income,
                                 @JsonProperty("benefits") final Benefits benefits,
                                 @JsonProperty("employmentStatus") final String employmentStatus) {
        this.defendantId = defendantId;
        this.income = income;
        this.benefits = benefits;
        this.employmentStatus = employmentStatus;
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
}