package uk.gov.moj.cpp.sjp.domain.onlineplea;

import uk.gov.moj.cpp.sjp.domain.PleaType;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Offence implements Serializable {
    private String id;
    private PleaType plea;
    private Boolean comeToCourt;
    private String mitigation;
    private String notGuiltyBecause;

    public Offence() {}

    @JsonCreator
    public Offence(@JsonProperty("id") final String id,
                   @JsonProperty("plea") final PleaType plea,
                   @JsonProperty("comeToCourt") final Boolean comeToCourt,
                   @JsonProperty("mitigation") final String mitigation,
                   @JsonProperty("notGuiltyBecause") final String notGuiltyBecause) {
        this.id = id;
        this.plea = plea;
        this.comeToCourt = comeToCourt;
        this.mitigation = mitigation;
        this.notGuiltyBecause = notGuiltyBecause;
    }

    public String getId() {
        return id;
    }

    public PleaType getPlea() {
        return plea;
    }

    public Boolean getComeToCourt() {
        return comeToCourt;
    }

    public String getMitigation() {
        return mitigation;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

}