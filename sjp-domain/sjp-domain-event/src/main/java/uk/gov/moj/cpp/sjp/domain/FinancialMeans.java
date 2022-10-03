package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.math.BigDecimal;
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

    private final BigDecimal  grossTurnover;

    private final BigDecimal  netTurnover;

    private final Integer numberOfEmployees;

    private final Boolean tradingMoreThanTwelveMonths;

    @JsonCreator
    public FinancialMeans(@JsonProperty("defendantId") final UUID defendantId,
                          @JsonProperty("income") final Income income,
                          @JsonProperty("benefits") final Benefits benefits,
                          @JsonProperty("employmentStatus") final String employmentStatus,
                          @JsonProperty("grossTurnover")  final BigDecimal grossTurnover,
                          @JsonProperty("netTurnover")  final BigDecimal netTurnover,
                          @JsonProperty("numberOfEmployees") final Integer numberOfEmployees,
                          @JsonProperty("tradingMoreThanTwelveMonths") final Boolean tradingMoreThanTwelveMonths) {
        this.defendantId = defendantId;
        this.income = income;
        this.benefits = benefits;
        this.employmentStatus = employmentStatus;
        this.grossTurnover = grossTurnover;
        this.netTurnover = netTurnover;
        this.numberOfEmployees = numberOfEmployees;
        this.tradingMoreThanTwelveMonths = tradingMoreThanTwelveMonths;
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

    public BigDecimal getGrossTurnover() {
        return grossTurnover;
    }

    public BigDecimal getNetTurnover() {
        return netTurnover;
    }

    public Integer getNumberOfEmployees() {
        return numberOfEmployees;
    }

    public Boolean getTradingMoreThanTwelveMonths() {
        return tradingMoreThanTwelveMonths;
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
        final boolean isFinancialMeansEquals = Objects.equals(grossTurnover, that.grossTurnover) && Objects.equals(netTurnover, that.netTurnover) && Objects.equals(numberOfEmployees, that.numberOfEmployees) && Objects.equals(tradingMoreThanTwelveMonths, that.tradingMoreThanTwelveMonths);
        return Objects.equals(defendantId, that.defendantId) && Objects.equals(income, that.income) && Objects.equals(benefits, that.benefits) && Objects.equals(employmentStatus, that.employmentStatus) && isFinancialMeansEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantId, income, benefits, employmentStatus, grossTurnover, netTurnover, numberOfEmployees, tradingMoreThanTwelveMonths);
    }
}
