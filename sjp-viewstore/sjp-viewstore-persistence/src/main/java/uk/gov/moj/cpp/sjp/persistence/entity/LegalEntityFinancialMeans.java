package uk.gov.moj.cpp.sjp.persistence.entity;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.checkerframework.checker.index.qual.Positive;

@Embeddable
public class LegalEntityFinancialMeans {
    @Column(name = "trading_more_than_twelve_months")
    private Boolean tradingMoreThan12Months;

    @Positive
    @Column(name = "number_of_employees")
    private Integer numberOfEmployees;

    @Column(name = "gross_turnover_whole_pounds")
    private BigDecimal grossTurnover;

    @Column(name = "net_turnover_whole_pounds")
    private BigDecimal netTurnover;

    public LegalEntityFinancialMeans(final Boolean tradingMoreThan12Months, @Positive final Integer numberOfEmployees, final BigDecimal grossTurnover, final BigDecimal netTurnover) {
        this.tradingMoreThan12Months = tradingMoreThan12Months;
        this.numberOfEmployees = numberOfEmployees;
        this.grossTurnover = grossTurnover;
        this.netTurnover = netTurnover;
    }

    public LegalEntityFinancialMeans() {

    }

    public Boolean getTradingMoreThan12Months() {
        return tradingMoreThan12Months;
    }

    public void setTradingMoreThan12Months(final Boolean tradingMoreThan12Months) {
        this.tradingMoreThan12Months = tradingMoreThan12Months;
    }

    public Integer getNumberOfEmployees() {
        return numberOfEmployees;
    }

    public void setNumberOfEmployees(final Integer numberOfEmployees) {
        this.numberOfEmployees = numberOfEmployees;
    }

    public BigDecimal getGrossTurnover() {
        return grossTurnover;
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}


