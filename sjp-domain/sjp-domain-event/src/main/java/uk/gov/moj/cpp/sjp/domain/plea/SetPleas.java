package uk.gov.moj.cpp.sjp.domain.plea;

import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SetPleas {

    private final DefendantCourtOptions defendantCourtOptions;

    private final List<Plea> pleas;

    @JsonCreator
    public SetPleas(@JsonProperty("defendantCourtOptions") final DefendantCourtOptions defendantCourtOptions,
                    @JsonProperty("pleas") final List<Plea> pleas) {
        this.defendantCourtOptions = defendantCourtOptions;
        this.pleas = pleas;
    }

    public DefendantCourtOptions getDefendantCourtOptions() {
        return defendantCourtOptions;
    }

    public List<Plea> getPleas() {
        return pleas;
    }

    @Override
    @SuppressWarnings("squid:S00122")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        final SetPleas setPleas = (SetPleas) o;
        return Objects.equals(defendantCourtOptions, setPleas.defendantCourtOptions) &&
                pleas.equals(setPleas.pleas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantCourtOptions, pleas);
    }
}
