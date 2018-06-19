package uk.gov.moj.cpp.sjp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Case {

    private UUID id;
    private String urn;
    private String enterpriseId;
    private String ptiUrn;
    private String initiationCode;
    private String summonsCode;
    private ProsecutingAuthority prosecutingAuthority;
    private String libraOriginatingOrg;
    private String libraHearingLocation;
    private LocalDate dateOfHearing;
    private String timeOfHearing;
    private BigDecimal costs;
    private LocalDate postingDate;
    private Defendant defendant;

    @JsonCreator
    public Case(@JsonProperty("id") UUID id,
                @JsonProperty("urn") String urn,
                @JsonProperty("enterpriseId") String enterpriseId,
                @JsonProperty("ptiUrn") String ptiUrn,
                @JsonProperty("prosecutingAuthority") ProsecutingAuthority prosecutingAuthority,
                @JsonProperty("initiationCode") String initiationCode,
                @JsonProperty("summonsCode") String summonsCode,
                @JsonProperty("libraOriginatingOrg") String libraOriginatingOrg,
                @JsonProperty("libraHearingLocation") String libraHearingLocation,
                @JsonProperty("dateOfHearing") LocalDate dateOfHearing,
                @JsonProperty("timeOfHearing") String timeOfHearing,
                @JsonProperty("costs") BigDecimal costs,
                @JsonProperty("postingDate") LocalDate postingDate,
                @JsonProperty("defendant") Defendant defendant) {
        this.id = id;
        this.urn = urn;
        this.enterpriseId = enterpriseId;
        this.ptiUrn = ptiUrn;
        this.initiationCode = initiationCode;
        this.summonsCode = summonsCode;
        this.prosecutingAuthority = prosecutingAuthority;
        this.libraOriginatingOrg = libraOriginatingOrg;
        this.libraHearingLocation = libraHearingLocation;
        this.dateOfHearing = dateOfHearing;
        this.timeOfHearing = timeOfHearing;
        this.costs = costs;
        this.postingDate = postingDate;
        this.defendant = defendant;
    }

    public UUID getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public String getEnterpriseId() {
        return enterpriseId;
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

    public LocalDate getDateOfHearing() {
        return dateOfHearing;
    }

    public String getTimeOfHearing() {
        return timeOfHearing;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public Defendant getDefendant() {
        return defendant;
    }
}
