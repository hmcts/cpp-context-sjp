package uk.gov.moj.cpp.sjp.event.decision;


import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.decision.resubmit.PaymentTermsInfo;

import java.io.Serializable;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(DecisionResubmitted.EVENT_NAME)
public class DecisionResubmitted implements Serializable {

    public static final String EVENT_NAME = "sjp.events.decision-resubmitted";

    private static final long serialVersionUID = -6536727603846104446L;

    private DecisionSaved decisionSaved;

    private ZonedDateTime resubmittedAt;

    private PaymentTermsInfo paymentTermsInfo;

    @JsonCreator
    public DecisionResubmitted(@JsonProperty("decisionSaved") final DecisionSaved decisionSaved,
                               @JsonProperty("resubmittedAt") final ZonedDateTime resubmittedAt,
                               @JsonProperty("paymentTermsInfo") final PaymentTermsInfo paymentTermsInfo) {
        this.decisionSaved = decisionSaved;
        this.resubmittedAt = resubmittedAt;
        this.paymentTermsInfo = paymentTermsInfo;
    }

    public DecisionSaved getDecisionSaved() {
        return decisionSaved;
    }

    public ZonedDateTime getResubmittedAt() {
        return resubmittedAt;
    }

    public PaymentTermsInfo getPaymentTermsInfo() {
        return paymentTermsInfo;
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}

