package uk.gov.moj.cpp.sjp.domain;

import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefendantCourtOptions implements Serializable {

    private static final long serialVersionUID = -1895094857439544269L;

    private final DefendantCourtInterpreter interpreter;

    private final Boolean welshHearing;

    private final DisabilityNeeds disabilityNeeds;

    @JsonCreator
    public DefendantCourtOptions(@JsonProperty("interpreter") final DefendantCourtInterpreter interpreter,
                                 @JsonProperty("welshHearing") final Boolean welshHearing,
                                 @JsonProperty("disabilityNeeds") final DisabilityNeeds disabilityNeeds) {
        this.interpreter = interpreter;
        this.welshHearing = welshHearing;
        this.disabilityNeeds = disabilityNeeds;
    }

    public DefendantCourtInterpreter getInterpreter() {
        return interpreter;
    }

    public Boolean getWelshHearing() {
        return welshHearing;
    }

    public DisabilityNeeds getDisabilityNeeds() {
        return disabilityNeeds;
    }

    @Override
    @SuppressWarnings("squid:S00122")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantCourtOptions that = (DefendantCourtOptions) o;
        return interpreter.equals(that.interpreter) &&
                welshHearing.equals(that.welshHearing) &&
                Objects.equals(disabilityNeeds, that.disabilityNeeds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interpreter, welshHearing, disabilityNeeds);
    }
}
