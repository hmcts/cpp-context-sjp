package uk.gov.moj.cpp.sjp.domain.onlineplea;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Offence implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private PleaType plea;
    private String mitigation;
    private String notGuiltyBecause;

    @JsonCreator
    public Offence(@JsonProperty("id") final UUID id,
                   @JsonProperty("plea") final PleaType plea,
                   @JsonProperty("mitigation") final String mitigation,
                   @JsonProperty("notGuiltyBecause") final String notGuiltyBecause) {
        this.id = id;
        this.plea = plea;
        this.mitigation = mitigation;
        this.notGuiltyBecause = notGuiltyBecause;
    }

    public UUID getId() {
        return id;
    }

    public PleaType getPlea() {
        return plea;
    }

    public String getMitigation() {
        return mitigation;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

}