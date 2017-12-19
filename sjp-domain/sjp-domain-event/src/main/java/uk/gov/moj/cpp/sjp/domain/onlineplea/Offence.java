package uk.gov.moj.cpp.sjp.domain.onlineplea;

import java.io.Serializable;

public class Offence implements Serializable {
    private final String id;
    private final String plea;
    private final Boolean comeToCourt;
    private final String mitigation;
    private final String notGuiltyBecause;

    public Offence(final String id, final String plea, final Boolean comeToCourt, final String mitigation, final String notGuiltyBecause) {
        this.id = id;
        this.plea = plea;
        this.comeToCourt = comeToCourt;
        this.mitigation = mitigation;
        this.notGuiltyBecause = notGuiltyBecause;
    }

    public String getId() {
        return id;
    }

    public String getPlea() {
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