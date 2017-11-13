package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.SjpOffence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Event("sjp.events.sjp-case-created")
public class SjpCaseCreated {

    private String id;
    private String urn;
    private String ptiUrn;
    private String initiationCode;
    private String summonsCode;
    private ProsecutingAuthority prosecutingAuthority;
    private String libraOriginatingOrg;
    private String libraHearingLocation;
    private LocalDate dateOfHearing;
    private String timeOfHearing;
    private String personId;
    private UUID defendantId;
    private BigDecimal costs;
    private int numPreviousConvictions;
    private LocalDate postingDate;
    private List<SjpOffence> offences;
    private ZonedDateTime createdOn;


    public SjpCaseCreated() {
    }

    public SjpCaseCreated(String id,
                          String urn,
                          String ptiUrn,
                          String initiationCode,
                          String summonsCode,
                          ProsecutingAuthority prosecutingAuthority,
                          String libraOriginatingOrg,
                          String libraHearingLocation,
                          LocalDate dateOfHearing,
                          String timeOfHearing,
                          String personId,
                          UUID defendantId,
                          int numPreviousConvictions,
                          BigDecimal costs,
                          LocalDate postingDate,
                          List<SjpOffence> offences,
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
        this.personId = personId;
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

    public String getPersonId() {
        return personId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public List<SjpOffence> getOffences() {
        return offences;
    }

    public void setOffences(List<SjpOffence> offences) {
        this.offences = offences;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SjpCaseCreated that = (SjpCaseCreated) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(urn, that.urn) &&
                Objects.equals(ptiUrn, that.ptiUrn) &&
                Objects.equals(initiationCode, that.initiationCode) &&
                Objects.equals(summonsCode, that.summonsCode) &&
                Objects.equals(prosecutingAuthority, that.prosecutingAuthority) &&
                Objects.equals(libraOriginatingOrg, that.libraOriginatingOrg) &&
                Objects.equals(libraHearingLocation, that.libraHearingLocation) &&
                Objects.equals(dateOfHearing, that.dateOfHearing) &&
                Objects.equals(timeOfHearing, that.timeOfHearing) &&
                Objects.equals(personId, that.personId) &&
                Objects.equals(numPreviousConvictions, that.numPreviousConvictions) &&
                Objects.equals(costs, that.costs) &&
                Objects.equals(postingDate, that.postingDate) &&
                Objects.equals(offences, that.offences) &&
                Objects.equals(createdOn, that.createdOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, urn, ptiUrn, initiationCode, summonsCode, prosecutingAuthority, libraOriginatingOrg, libraHearingLocation, dateOfHearing, timeOfHearing, personId, numPreviousConvictions, costs, postingDate, offences, createdOn);
    }

    @Override
    public String toString() {
        return "SjpCaseCreated{" +
                "id='" + id + '\'' +
                ", urn='" + urn + '\'' +
                ", ptiUrn='" + ptiUrn + '\'' +
                ", initiationCode='" + initiationCode + '\'' +
                ", summonsCode='" + summonsCode + '\'' +
                ", prosecutingAuthority='" + prosecutingAuthority + '\'' +
                ", libraOriginatingOrg='" + libraOriginatingOrg + '\'' +
                ", libraHearingLocation='" + libraHearingLocation + '\'' +
                ", dateOfHearing=" + dateOfHearing +
                ", timeOfHearing='" + timeOfHearing + '\'' +
                ", personId='" + personId + '\'' +
                ", numPreviousConvictions='" + numPreviousConvictions + '\'' +
                ", costs='" + costs + '\'' +
                ", offences=" + offences +
                ", postingDate=" + postingDate +
                ", createdOn=" + createdOn +
                '}';
    }

}
