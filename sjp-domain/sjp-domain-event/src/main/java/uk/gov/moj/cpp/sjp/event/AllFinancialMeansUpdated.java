package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.Income;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.all-financial-means-updated")
public class AllFinancialMeansUpdated {

    private final UUID defendantId;
    private final Benefits benefits;
    private final String employmentStatus;
    private final Employer employer;
    private final Income income;

    @JsonCreator
    public AllFinancialMeansUpdated(@JsonProperty("defendantId") final UUID defendantId,
                                    @JsonProperty("income") final Income income,
                                    @JsonProperty("benefits") final Benefits benefits,
                                    @JsonProperty("employmentStatus") final String employmentStatus,
                                    @JsonProperty("employer") final Employer employer) {

        this.defendantId = defendantId;
        this.income = income;
        this.benefits = benefits;
        this.employmentStatus = employmentStatus;
        this.employer = employer;
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

    public Employer getEmployer() {
        return employer;
    }

}


