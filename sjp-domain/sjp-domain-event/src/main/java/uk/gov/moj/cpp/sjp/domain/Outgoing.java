package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Outgoing implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String description;
    private final BigDecimal amount;

    @JsonCreator
    public Outgoing(@JsonProperty("description") final String description,
                    @JsonProperty("amount") final BigDecimal amount) {
        this.description = description;
        this.amount = amount;
    }

    public String getDescription() {
        return description;
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
        final Outgoing that = (Outgoing) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, amount);
    }
}