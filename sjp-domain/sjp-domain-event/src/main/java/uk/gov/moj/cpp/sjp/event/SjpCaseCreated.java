package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @deprecated Replaced by {@link CaseReceived}
 */
@Event("sjp.events.sjp-case-created")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SjpCaseCreated {

    private final UUID id;
    private final String urn;
    private final String ptiUrn;
    private final String initiationCode;
    private final String summonsCode;
    private final ProsecutingAuthority prosecutingAuthority;
    private final String libraOriginatingOrg;
    private final String libraHearingLocation;
    private final LocalDate dateOfHearing;
    private final String timeOfHearing;
    private final UUID defendantId;
    private final BigDecimal costs;
    private final int numPreviousConvictions;
    private final LocalDate postingDate;
    private final List<Offence> offences;
    private final ZonedDateTime createdOn;

    @JsonCreator
    @SuppressWarnings("squid:S00107")
    public SjpCaseCreated(
            @JsonProperty("id") final UUID id,
            @JsonProperty("urn") final String urn,
            @JsonProperty("ptiUrn") final String ptiUrn,
            @JsonProperty("initiationCode") final String initiationCode,
            @JsonProperty("summonsCode") final String summonsCode,
            @JsonProperty("prosecutingAuthority") final ProsecutingAuthority prosecutingAuthority,
            @JsonProperty("libraOriginatingOrg") final String libraOriginatingOrg,
            @JsonProperty("libraHearingLocation") final String libraHearingLocation,
            @JsonProperty("dateOfHearing") final LocalDate dateOfHearing,
            @JsonProperty("timeOfHearing") final String timeOfHearing,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("numPreviousConvictions") final int numPreviousConvictions,
            @JsonProperty("costs") final BigDecimal costs,
            @JsonProperty("postingDate") final LocalDate postingDate,
            @JsonProperty("offences") final List<Offence> offences,
            @JsonProperty("createdOn") final ZonedDateTime createdOn) {
        this.urn = urn;
        this.id = id;
        this.ptiUrn = ptiUrn;
        this.initiationCode = initiationCode;
        this.summonsCode = summonsCode;
        this.prosecutingAuthority = prosecutingAuthority;
        this.libraOriginatingOrg = libraOriginatingOrg;
        this.libraHearingLocation = libraHearingLocation;
        this.dateOfHearing = dateOfHearing;
        this.timeOfHearing = timeOfHearing;
        this.defendantId = defendantId;
        this.numPreviousConvictions = numPreviousConvictions;
        this.costs = costs;
        this.postingDate = postingDate;
        this.offences = offences;
        this.createdOn = createdOn;
    }

    public String getUrn() {
        return urn;
    }

    public String getPtiUrn() {
        return ptiUrn;
    }

    public String getInitiationCode() {
        return initiationCode;
    }

    public String getSummonsCode() {
        return summonsCode;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public String getLibraOriginatingOrg() {
        return libraOriginatingOrg;
    }

    public String getLibraHearingLocation() {
        return libraHearingLocation;
    }

    public String getTimeOfHearing() {
        return timeOfHearing;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDateOfHearing() {
        return dateOfHearing;
    }

    public int getNumPreviousConvictions() {
        return numPreviousConvictions;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public ZonedDateTime getCreatedOn() {
        return createdOn;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
