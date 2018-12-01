package uk.gov.moj.cpp.sjp.event;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event("sjp.events.defendant-national-insurance-number-updated")
public class DefendantsNationalInsuranceNumberUpdated {

    private final UUID caseId;
    private final UUID defendantId;
    private final String nationalInsuranceNumber;

    @JsonCreator
    public DefendantsNationalInsuranceNumberUpdated(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefendantsNationalInsuranceNumberUpdated)) {
            return false;
        }
        final DefendantsNationalInsuranceNumberUpdated that = (DefendantsNationalInsuranceNumberUpdated) o;

        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(nationalInsuranceNumber, that.nationalInsuranceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantId, nationalInsuranceNumber);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
