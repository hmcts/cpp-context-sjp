package uk.gov.moj.cpp.sjp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Case {

    private final UUID id;
    private final String urn;
    private final String enterpriseId;
    private final ProsecutingAuthority prosecutingAuthority;
    private final BigDecimal costs;
    private final LocalDate postingDate;
    private final Defendant defendant;

    @JsonCreator
    public Case(@JsonProperty("id") final UUID id,
                @JsonProperty("urn") final String urn,
                @JsonProperty("enterpriseId") final String enterpriseId,
                @JsonProperty("prosecutingAuthority") final ProsecutingAuthority prosecutingAuthority,
                @JsonProperty("costs") final BigDecimal costs,
                @JsonProperty("postingDate") final LocalDate postingDate,
                @JsonProperty("defendant") final Defendant defendant) {
        this.id = id;
        this.urn = urn;
        this.enterpriseId = enterpriseId;
        this.prosecutingAuthority = prosecutingAuthority;
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

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
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
