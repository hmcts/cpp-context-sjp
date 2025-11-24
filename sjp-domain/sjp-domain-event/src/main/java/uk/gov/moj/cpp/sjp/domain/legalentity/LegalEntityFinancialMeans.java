package uk.gov.moj.cpp.sjp.domain.legalentity;

import java.math.BigDecimal;

public class LegalEntityFinancialMeans {
    private Boolean tradingMoreThan12Months;

    private Integer numberOfEmployees;

    private BigDecimal grossTurnover;

    private BigDecimal netTurnover;

    private Boolean outstandingFines;

    public Boolean getTradingMoreThan12Months() {
        return tradingMoreThan12Months;
    }

    public Integer getNumberOfEmployees() {
        return numberOfEmployees;
    }

    public BigDecimal getGrossTurnover() {
        return grossTurnover;
    }

    public void setTradingMoreThan12Months(final Boolean tradingMoreThan12Months) {
        this.tradingMoreThan12Months = tradingMoreThan12Months;
    }

    public void setNumberOfEmployees(final Integer numberOfEmployees) {
        this.numberOfEmployees = numberOfEmployees;
    }

    public void setGrossTurnover(final BigDecimal grossTurnover) {
        this.grossTurnover = grossTurnover;
    }

    public BigDecimal getNetTurnover() {
        return netTurnover;
    }

    public void setNetTurnover(final BigDecimal netTurnover) {
        this.netTurnover = netTurnover;
    }

    public Boolean getOutstandingFines() {
        return outstandingFines;
    }

    public void setOutstandingFines(final Boolean outstandingFines) {
        this.outstandingFines = outstandingFines;
    }
}
