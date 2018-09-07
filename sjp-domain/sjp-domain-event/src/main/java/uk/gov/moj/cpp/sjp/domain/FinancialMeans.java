package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties("caseId")
public class FinancialMeans implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID defendantId;
    private final Income income;
    private final Benefits benefits;
    private final String employmentStatus;

    @JsonCreator
    public FinancialMeans(@JsonProperty("defendantId") final UUID defendantId,
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FinancialMeans that = (FinancialMeans) o;
        return Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(income, that.income) &&
                Objects.equals(benefits, that.benefits) &&
                Objects.equals(employmentStatus, that.employmentStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantId, income, benefits, employmentStatus);
    }
}
