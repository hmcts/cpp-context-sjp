package uk.gov.moj.cpp.sjp.domain.decision;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


@JsonTypeInfo(use = NAME, property = "type")
@JsonSubTypes({
        @Type(value = Dismiss.class, name = DecisionType.DecisionName.DISMISS),
        @Type(value = Withdraw.class, name = DecisionType.DecisionName.WITHDRAW),
        @Type(value = Adjourn.class, name = DecisionType.DecisionName.ADJOURN),
        @Type(value = ReferForCourtHearing.class, name = DecisionType.DecisionName.REFER_FOR_COURT_HEARING),
        @Type(value = Discharge.class, name = DecisionType.DecisionName.DISCHARGE),
        @Type(value = ReferredToOpenCourt.class, name = DecisionType.DecisionName.REFERRED_TO_OPEN_COURT),
        @Type(value = ReferredForFutureSJPSession.class, name = DecisionType.DecisionName.REFERRED_FOR_FUTURE_SJP_SESSION),
        @Type(value = FinancialPenalty.class, name = DecisionType.DecisionName.FINANCIAL_PENALTY),
        @Type(value = NoSeparatePenalty.class, name = DecisionType.DecisionName.NO_SEPARATE_PENALTY),
        @Type(value = SetAside.class, name = DecisionType.DecisionName.SET_ASIDE)
})
public abstract class OffenceDecision implements Serializable {

    private final UUID id;

    private final DecisionType type;

    private final PressRestriction pressRestriction;

    public OffenceDecision(final UUID id, final DecisionType type) {
        this(id, type, null);
    }

    public OffenceDecision(final UUID id, final DecisionType type, final PressRestriction pressRestriction) {
        this.id = id;
        this.type = type;
        this.pressRestriction = pressRestriction;
    }

    public UUID getId() {
        return this.id;
    }

    public DecisionType getType() {
        return type;
    }

    public PressRestriction getPressRestriction() {
        return pressRestriction;
    }

    @JsonIgnore
    public boolean hasPressRestriction() {
        return Objects.nonNull(this.pressRestriction);
    }

    @JsonIgnore
    public Boolean isFinalDecision() {
        return getType().isFinal();
    }

    @JsonIgnore
    public Boolean isNotFinalDecision() {
        return !isFinalDecision();
    }

    public abstract void accept(final OffenceDecisionVisitor visitor);

    /**
     * @return a list of offence ids applicable to this offence decision.
     */
    @JsonIgnore
    public List<UUID> getOffenceIds() {
        return offenceDecisionInformationAsList().stream()
                .map(OffenceDecisionInformation::getOffenceId)
                .collect(toList());
    }

    /**
     * Accesor/helper method to get all available offences decisions information
     * @return all available offence-decision-information objects
     */
    @JsonIgnore
    public abstract List<OffenceDecisionInformation> offenceDecisionInformationAsList();

    @JsonIgnore
    public Optional<OffenceDecisionInformation> getOffenceDecisionInformation(final UUID offenceId){
        return offenceDecisionInformationAsList()
                .stream()
                .filter(offenceDecisionInformation -> offenceDecisionInformation.getOffenceId().equals(offenceId))
                .findFirst();
    }

    @JsonIgnore
    public boolean isConviction(final UUID offenceId) {
        return getOffenceDecisionInformation(offenceId)
                .map(OffenceDecisionInformation::isConviction)
                .orElse(false);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
