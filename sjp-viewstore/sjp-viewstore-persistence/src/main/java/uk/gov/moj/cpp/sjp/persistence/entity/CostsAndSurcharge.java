package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class CostsAndSurcharge implements Serializable {

    @Column(name = "costs")
    private BigDecimal costs;

    @Column(name = "reason_for_no_costs")
    private String reasonForNoCosts;

    @Column(name = "victim_surcharge")
    private BigDecimal victimSurcharge;

    @Column(name = "reason_for_no_victim_surcharge")
    private String reasonForNoVictimSurcharge;

    @Column(name = "collection_order_made")
    private boolean collectionOrderMade;

    @Column(name = "reason_for_reduced_victim_surcharge")
    private String reasonForReducedVictimSurcharge;

    public CostsAndSurcharge() {
    }

    public CostsAndSurcharge(final BigDecimal costs, final String reasonForNoCosts, final BigDecimal victimSurcharge,
                             final String reasonForNoVictimSurcharge, final boolean collectionOrderMade,
                             final String reasonForReducedVictimSurcharge) {
        this.costs = costs;
        this.reasonForNoCosts = reasonForNoCosts;
        this.victimSurcharge = victimSurcharge;
        this.reasonForNoVictimSurcharge = reasonForNoVictimSurcharge;
        this.collectionOrderMade = collectionOrderMade;
        this.reasonForReducedVictimSurcharge = reasonForReducedVictimSurcharge;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public void setCosts(final BigDecimal costs) {
        this.costs = costs;
    }

    public String getReasonForNoCosts() {
        return reasonForNoCosts;
    }

    public void setReasonForNoCosts(final String reasonForNoCosts) {
        this.reasonForNoCosts = reasonForNoCosts;
    }

    public BigDecimal getVictimSurcharge() {
        return victimSurcharge;
    }

    public void setVictimSurcharge(final BigDecimal victimSurcharge) {
        this.victimSurcharge = victimSurcharge;
    }

    public String getReasonForNoVictimSurcharge() {
        return reasonForNoVictimSurcharge;
    }

    public void setReasonForNoVictimSurcharge(final String reasonForNoVictimSurcharge) {
        this.reasonForNoVictimSurcharge = reasonForNoVictimSurcharge;
    }

    public boolean isCollectionOrderMade() {
        return collectionOrderMade;
    }

    public void setCollectionOrderMade(final boolean collectionOrderMade) {
        this.collectionOrderMade = collectionOrderMade;
    }

    public void setReasonForReducedVictimSurcharge(String reasonForReducedVictimSurcharge) {
        this.reasonForReducedVictimSurcharge = reasonForReducedVictimSurcharge;
    }

    public String getReasonForReducedVictimSurcharge() {
        return reasonForReducedVictimSurcharge;
    }
}
