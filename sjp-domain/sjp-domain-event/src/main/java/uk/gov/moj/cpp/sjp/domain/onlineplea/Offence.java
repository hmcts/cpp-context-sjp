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

    public static Builder builder() {
        return new Offence.Builder();
    }

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

    public static final class Builder {
        private UUID id;
        private PleaType plea;
        private String mitigation;
        private String notGuiltyBecause;


        public Offence.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Offence.Builder withPleaType(final PleaType plea) {
            this.plea = plea;
            return this;
        }

        public Offence.Builder withMitigation(final String mitigation) {
            this.mitigation = mitigation;
            return this;
        }
        public Offence.Builder withNotGuiltyBecause(final String notGuiltyBecause) {
            this.notGuiltyBecause = notGuiltyBecause;
            return this;
        }

        public Builder withValuesFrom(final Offence offence) {
            this.id = offence.getId();
            this.plea = offence.getPlea();
            this.mitigation = offence.getMitigation();
            this.notGuiltyBecause = offence.getNotGuiltyBecause();
            return this;
        }

        public Offence build() {
            return new Offence(id, plea, mitigation, notGuiltyBecause);
        }

    }


}