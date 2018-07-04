package uk.gov.moj.cpp.sjp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Case {

    private UUID id;
    private String urn;
    private String enterpriseId;
    private ProsecutingAuthority prosecutingAuthority;
    private BigDecimal costs;
    private LocalDate postingDate;
    private Defendant defendant;

    @JsonCreator
    public Case(@JsonProperty("id") UUID id,
                @JsonProperty("urn") String urn,
                @JsonProperty("enterpriseId") String enterpriseId,
                @JsonProperty("prosecutingAuthority") ProsecutingAuthority prosecutingAuthority,
                @JsonProperty("costs") BigDecimal costs,
                @JsonProperty("postingDate") LocalDate postingDate,
                @JsonProperty("defendant") Defendant defendant) {
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
