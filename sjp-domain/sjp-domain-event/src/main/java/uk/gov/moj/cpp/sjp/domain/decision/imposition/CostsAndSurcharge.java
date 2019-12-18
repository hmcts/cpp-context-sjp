package uk.gov.moj.cpp.sjp.domain.decision.imposition;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CostsAndSurcharge implements Serializable {

    private BigDecimal costs;

    private String reasonForNoCosts;

    private BigDecimal victimSurcharge;

    private String reasonForNoVictimSurcharge;

    private String reasonForReducedVictimSurcharge;

    private boolean collectionOrderMade;


    @JsonCreator
    public CostsAndSurcharge(@JsonProperty("costs") final BigDecimal costs,
                             @JsonProperty("reasonForNoCosts") final String reasonForNoCosts,
                             @JsonProperty("victimSurcharge") final BigDecimal victimSurcharge,
                             @JsonProperty("reasonForNoVictimSurcharge") final String reasonForNoVictimSurcharge,
                             @JsonProperty("reasonForReducedVictimSurcharge") final String reasonForReducedVictimSurcharge,
                             @JsonProperty("collectionOrderMade") final boolean collectionOrderMade) {
        this.costs = costs;
        this.reasonForNoCosts = reasonForNoCosts;
        this.victimSurcharge = victimSurcharge;
        this.reasonForNoVictimSurcharge = reasonForNoVictimSurcharge;
        this.reasonForReducedVictimSurcharge = reasonForReducedVictimSurcharge;
        this.collectionOrderMade = collectionOrderMade;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public String getReasonForNoCosts() {
        return reasonForNoCosts;
    }

    public BigDecimal getVictimSurcharge() {
        return victimSurcharge;
    }

    public String getReasonForNoVictimSurcharge() {
        return reasonForNoVictimSurcharge;
    }

    public boolean isCollectionOrderMade() {
        return collectionOrderMade;
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    public String getReasonForReducedVictimSurcharge() {
        return reasonForReducedVictimSurcharge;
    }
}
