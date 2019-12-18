package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LumpSum implements Serializable {

    @Column(name = "lump_sum_amount")
    private BigDecimal amount;

    @Column(name = "lump_sum_within_days")
    private Integer withinDays;

    @Column(name = "lump_sum_pay_by_date")
    private LocalDate payByDate;

    public LumpSum() {
    }

    public LumpSum(BigDecimal amount, Integer withinDays, LocalDate payByDate) {
        this.amount = amount;
        this.withinDays = withinDays;
        this.payByDate = payByDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getWithinDays() {
        return withinDays;
    }

    public void setWithinDays(final Integer withinDays) {
        this.withinDays = withinDays;
    }

    public LocalDate getPayByDate() {
        return payByDate;
    }

    public void setPayByDate(final LocalDate payByDate) {
        this.payByDate = payByDate;
    }
}
