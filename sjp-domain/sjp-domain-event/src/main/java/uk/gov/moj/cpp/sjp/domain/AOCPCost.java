package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AOCPCost implements Serializable {

    private static final long serialVersionUID = 3311654497408134809L;

    private final UUID id;
    private final BigDecimal costs;
    private final AOCPCostDefendant defendant;

    @JsonCreator
    public AOCPCost(@JsonProperty("id") final UUID id,
                    @JsonProperty("costs") final BigDecimal costs,
                    @JsonProperty("defendant") final AOCPCostDefendant defendant) {
       this.id = id;
       this.costs = costs;
       this.defendant = defendant;

    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public AOCPCostDefendant getDefendant() {
        return defendant;
    }
}
