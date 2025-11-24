package uk.gov.moj.cpp.sjp.domain.plea;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Plea implements Serializable {

    private static final long serialVersionUID = -7030217224530242312L;

    private UUID defendantId;

    private UUID offenceId;

    private PleaType pleaType;

    private String notGuiltyBecause;

    private String mitigation;

    @JsonCreator
    public Plea(@JsonProperty("defendantId") final UUID defendantId,
                @JsonProperty("offenceId") final UUID offenceId,
                @JsonProperty("pleaType") final PleaType pleaType,
                @JsonProperty("notGuiltyBecause") String notGuiltyBecause,
                @JsonProperty("mitigation") String mitigation) {
        this.defendantId = defendantId;
        this.offenceId = offenceId;
        this.pleaType = pleaType;
        this.notGuiltyBecause = notGuiltyBecause;
        this.mitigation = mitigation;
    }

    public Plea(final UUID defendantId,
                final UUID offenceId,
                final PleaType pleaType) {
        this.defendantId = defendantId;
        this.offenceId = offenceId;
        this.pleaType = pleaType;
    }

    public Plea(final Plea plea) {
        this.defendantId = plea.getDefendantId();
        this.offenceId = plea.getOffenceId();
        this.pleaType = plea.getPleaType();
        this.notGuiltyBecause = plea.getNotGuiltyBecause();
        this.mitigation = plea.getMitigation();
    }


    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public PleaType getPleaType() {
        return pleaType;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

    public String getMitigation() {
        return mitigation;
    }

    @Override
    @SuppressWarnings("squid:S00122")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Plea plea = (Plea) o;
        return defendantId.equals(plea.defendantId) &&
                offenceId.equals(plea.offenceId) &&
                pleaType == plea.pleaType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantId, offenceId, pleaType);
    }
}
