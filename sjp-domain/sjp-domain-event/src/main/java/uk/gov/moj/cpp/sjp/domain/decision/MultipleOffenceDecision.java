package uk.gov.moj.cpp.sjp.domain.decision;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.util.List;
import java.util.UUID;

public abstract class MultipleOffenceDecision extends OffenceDecision {

    private final List<OffenceDecisionInformation> offenceDecisionInformation;

    public MultipleOffenceDecision(final UUID id,
                                   final DecisionType type,
                                   final List<OffenceDecisionInformation> offenceDecisionInformation) {
        this(id, type, offenceDecisionInformation, null);
    }

    public MultipleOffenceDecision(final UUID id,
                                   final DecisionType type,
                                   final List<OffenceDecisionInformation> offenceDecisionInformation,
                                   final PressRestriction pressRestriction) {
        super(id, type, pressRestriction);
        this.offenceDecisionInformation = offenceDecisionInformation;
    }

    public List<OffenceDecisionInformation> getOffenceDecisionInformation() {
        return offenceDecisionInformation;
    }

    @Override
    public List<OffenceDecisionInformation> offenceDecisionInformationAsList() {
        return unmodifiableList(offenceDecisionInformation);
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
