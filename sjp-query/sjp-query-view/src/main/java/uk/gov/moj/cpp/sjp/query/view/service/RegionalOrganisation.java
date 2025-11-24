package uk.gov.moj.cpp.sjp.query.view.service;

import java.util.UUID;

public class RegionalOrganisation {

    private UUID id;
    private Integer seqNum;
    private String regionName;
    private String cbwaEnforcerEmail;

    public RegionalOrganisation() {
    }

    public RegionalOrganisation(final UUID id,
                                final String regionName,
                                final Integer seqNum,
                                final String cbwaEnforcerEmail) {
        this.id = id;
        this.seqNum = seqNum;
        this.regionName = regionName;
        this.cbwaEnforcerEmail = cbwaEnforcerEmail;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Integer getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(final Integer seqNum) {
        this.seqNum = seqNum;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(final String regionName) {
        this.regionName = regionName;
    }

    public String getCbwaEnforcerEmail() {
        return cbwaEnforcerEmail;
    }

    public void setCbwaEnforcerEmail(final String cbwaEnforcerEmail) {
        this.cbwaEnforcerEmail = cbwaEnforcerEmail;
    }
}
