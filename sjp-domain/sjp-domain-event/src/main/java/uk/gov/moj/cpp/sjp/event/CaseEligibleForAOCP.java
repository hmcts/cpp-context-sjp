package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.AOCPCostDefendant;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(CaseEligibleForAOCP.EVENT_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseEligibleForAOCP implements Serializable {

    private static final long serialVersionUID = 6201105614626835159L;
    public static final String EVENT_NAME = "sjp.events.case-eligible-for-aocp";

    private final UUID caseId;
    private final BigDecimal costs;
    private final BigDecimal victimSurcharge;
    private final BigDecimal aocpTotalCost;
    private final AOCPCostDefendant aocpCostDefendant;


    @JsonCreator
    public CaseEligibleForAOCP(@JsonProperty("caseId") UUID caseId,
                               @JsonProperty("costs") BigDecimal costs,
                               @JsonProperty("victimSurcharge") BigDecimal victimSurcharge,
                               @JsonProperty("aocpTotalCost") BigDecimal aocpTotalCost,
                               @JsonProperty("defendant") AOCPCostDefendant aocpCostDefendant) {
        this.caseId = caseId;
        this.costs = costs;
        this.victimSurcharge = victimSurcharge;
        this.aocpTotalCost = aocpTotalCost;
        this.aocpCostDefendant = aocpCostDefendant;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public BigDecimal getVictimSurcharge() {
        return victimSurcharge;
    }

    public BigDecimal getAocpTotalCost() {
        return aocpTotalCost;
    }

    public AOCPCostDefendant getAocpCostDefendant() {
        return aocpCostDefendant;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
