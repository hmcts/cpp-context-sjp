package uk.gov.moj.cpp.sjp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Case {

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
    private String personId;
    private List<SjpOffence> offences;
    private int numPreviousConvictions;
    private BigDecimal costs;
    private LocalDate postingDate;

    public Case(UUID id,
                String urn,
                String ptiUrn,
                ProsecutingAuthority prosecutingAuthority,
                String initiationCode,
                String summonsCode,
                String libraOriginatingOrg,
                String libraHearingLocation,
                LocalDate dateOfHearing,
                String timeOfHearing,
                String personId,
                int numPreviousConvictions,
                BigDecimal costs,
                LocalDate postingDate,
                List<SjpOffence> offences) {
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
        this.offences = offences;
        this.numPreviousConvictions = numPreviousConvictions;
        this.costs = costs;
        this.postingDate = postingDate;
    }

    public String getUrn() {
        return urn;
    }

    public UUID getId() {
        return id;
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

    public String getPersonId() {
        return personId;
    }

    public List<SjpOffence> getOffences() {
        return offences;
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
}
