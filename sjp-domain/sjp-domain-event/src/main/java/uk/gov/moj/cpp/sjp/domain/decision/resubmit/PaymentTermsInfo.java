package uk.gov.moj.cpp.sjp.domain.decision.resubmit;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentTermsInfo implements Serializable {

    private static final long serialVersionUID = 7193056950237733751L;

    private Integer numberOfDaysToPostponeBy;

    private boolean resetPayByDate;

    @JsonCreator
    public PaymentTermsInfo(@JsonProperty("numberOfDaysToPostponeBy") final Integer numberOfDaysToPostponeBy,
                            @JsonProperty("resetPayByDate") final boolean resetPayByDate) {
        this.numberOfDaysToPostponeBy = numberOfDaysToPostponeBy;
        this.resetPayByDate = resetPayByDate;
    }

    public Integer getNumberOfDaysToPostponeBy() {
        return numberOfDaysToPostponeBy;
    }

    public boolean isResetPayByDate() {
        return resetPayByDate;
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }
}
