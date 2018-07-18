package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @deprecated Replaced by {@link CaseReceived}
 */
@Event("sjp.events.sjp-case-created")
@JsonIgnoreProperties("personId")
public class SjpCaseCreated {

    private UUID id;
    private String urn;
    private String ptiUrn;
    private String initiationCode;
    private String summonsCode;
    private ProsecutingAuthority prosecutingAuthority;
    private String libraOriginatingOrg;
    private String libraHearingLocation;
    private LocalDate dateOfHearing;
    private String timeOfHearing;
    private UUID defendantId;
    private BigDecimal costs;
    private int numPreviousConvictions;
    private LocalDate postingDate;
    private List<Offence> offences;
    private ZonedDateTime createdOn;


    public SjpCaseCreated() {
    }

    public SjpCaseCreated(UUID id,
                          String urn,
                          String ptiUrn,
                          String initiationCode,
                          String summonsCode,
                          ProsecutingAuthority prosecutingAuthority,
                          String libraOriginatingOrg,
                          String libraHearingLocation,
                          LocalDate dateOfHearing,
                          String timeOfHearing,
                          UUID defendantId,
                          int numPreviousConvictions,
                          BigDecimal costs,
                          LocalDate postingDate,
                          List<Offence> offences,
                          ZonedDateTime createdOn) {
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

    public void setUrn(String urn) {
        this.urn = urn;
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

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public void setOffences(List<Offence> offences) {
        this.offences = offences;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setCosts(BigDecimal costs) {
        this.costs = costs;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public ZonedDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(ZonedDateTime createdOn) {
        this.createdOn = createdOn;
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
