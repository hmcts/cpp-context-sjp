package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class Installments implements Serializable {

    @Column(name = "instalments_amount")
    private BigDecimal amount;

    @Column(name = "instalments_period")
    @Enumerated(EnumType.STRING)
    private InstallmentPeriod period;

    @Column(name = "instalments_start_date")
    private LocalDate startDate;

    public Installments(){}

    public Installments(BigDecimal amount, InstallmentPeriod period, LocalDate startDate) {
        this.amount = amount;
        this.period = period;
        this.startDate = startDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public InstallmentPeriod getPeriod() {
        return period;
    }

    public void setPeriod(final InstallmentPeriod installmentPeriod) {
        this.period = installmentPeriod;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }
}
