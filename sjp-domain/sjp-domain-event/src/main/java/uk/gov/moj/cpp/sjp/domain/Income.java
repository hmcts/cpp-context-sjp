package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class Income implements Serializable {

    private static final long serialVersionUID = 1L;
    private final IncomeFrequency frequency;
    private final BigDecimal amount;

    public Income(final IncomeFrequency frequency, final BigDecimal amount) {
        this.frequency = frequency;
        this.amount = amount;
    }

    public IncomeFrequency getFrequency() {
        return frequency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Income income = (Income) o;
        return frequency == income.frequency &&
                Objects.equals(amount, income.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequency, amount);
    }

}