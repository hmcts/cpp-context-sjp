package uk.gov.moj.cpp.sjp.event;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(PaymentTermsChanged.EVENT_NAME)
public class PaymentTermsChanged implements Serializable {

    public static final String EVENT_NAME = "sjp.events.payment-terms-changed";

    private static final long serialVersionUID = 2347210046678397612L;

    private final PaymentTerms oldPaymentTerms;

    private final PaymentTerms newPaymentTerms;

    @JsonCreator
    public PaymentTermsChanged(@JsonProperty("oldPaymentTerms") PaymentTerms oldPaymentTerms,
                               @JsonProperty("newPaymentTerms") PaymentTerms newPaymentTerms) {
        this.oldPaymentTerms = oldPaymentTerms;
        this.newPaymentTerms = newPaymentTerms;
    }

    public PaymentTerms getOldPaymentTerms() {
        return oldPaymentTerms;
    }


    public PaymentTerms getNewPaymentTerms() {
        return newPaymentTerms;
    }

    @Override
    public String toString() {
        return reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
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
