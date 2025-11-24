package uk.gov.moj.cpp.sjp.domain;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import uk.gov.justice.core.courts.Hearing;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GrantedApplicationResults{
    private final Hearing hearing;

    private final String hearingDay;

    private final Boolean isReshare;

    private final List<UUID> shadowListedOffences;

    private final ZonedDateTime sharedTime;

    @JsonCreator
    public GrantedApplicationResults(@JsonProperty("hearing") final Hearing hearing,
                                     @JsonProperty("hearingDay") final String hearingDay,
                                     @JsonProperty("isReshare") final Boolean isReshare,
                                     @JsonProperty("shadowListedOffences") final List<UUID> shadowListedOffences,
                                     @JsonProperty("sharedTime") final ZonedDateTime sharedTime) {
       this.hearing = hearing;
       this.hearingDay = hearingDay;
       this.isReshare = isReshare;
       this.shadowListedOffences = shadowListedOffences;
       this.sharedTime = sharedTime;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public String getHearingDay() {
        return hearingDay;
    }

    public Boolean getReshare() {
        return isReshare;
    }

    public List<UUID> getShadowListedOffences() {
        return shadowListedOffences;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
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
