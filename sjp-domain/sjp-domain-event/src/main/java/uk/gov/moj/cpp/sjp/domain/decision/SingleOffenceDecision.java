package uk.gov.moj.cpp.sjp.domain.decision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;


public abstract class SingleOffenceDecision extends OffenceDecision {

    private OffenceDecisionInformation offenceDecisionInformation;

    @JsonCreator
    public SingleOffenceDecision(@JsonProperty("id") final UUID id,
                                 @JsonProperty("type") final DecisionType type,
                                 @JsonProperty("offenceDecisionInformation") OffenceDecisionInformation offenceDecisionInformation) {
        super(id, type);
        if(offenceDecisionInformation == null){
            throw new IllegalArgumentException("invalid single offence decision");
        }
        this.offenceDecisionInformation = offenceDecisionInformation;
    }

    public OffenceDecisionInformation getOffenceDecisionInformation(){
        return offenceDecisionInformation;
    }

    @Override
    @JsonIgnore
    public List<OffenceDecisionInformation> offenceDecisionInformationAsList() {
        return singletonList(offenceDecisionInformation);
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
