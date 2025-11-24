package uk.gov.moj.cpp.sjp.event.decision;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event(DecisionRejected.EVENT_NAME)
public class DecisionRejected {

    public static final String EVENT_NAME = "sjp.events.decision-rejected";
    private final List<String> rejectionReasons;
    private Decision decision;

    @JsonCreator
    public DecisionRejected(@JsonProperty("decision") final Decision decision,
                            @JsonProperty("rejectionReasons") final List<String> rejectionReasons) {
        this.decision = decision;
        this.rejectionReasons = rejectionReasons;
    }

    public List<String> getRejectionReasons() {
        return rejectionReasons;
    }

    public Decision getDecision() {
        return decision;
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
        return "DecisionRejected{" +
                "rejectionReasons=" + rejectionReasons +
                ", decision=" + decision +
                '}';
    }
}
