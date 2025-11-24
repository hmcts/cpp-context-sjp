package uk.gov.moj.cpp.sjp.event.decision;


import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingInformation;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(ConvictionCourtResolved.EVENT_NAME)
public class ConvictionCourtResolved implements Serializable {

    private static final long serialVersionUID = 6642617575162421150L;

    public static final String EVENT_NAME = "sjp.events.conviction-court-resolved";

    private UUID caseId;

    private List<ConvictingInformation> convictingInformations;

    @JsonCreator
    public ConvictionCourtResolved(@JsonProperty("caseId") final UUID caseId,
                                   @JsonProperty("resolvedConvictingInformation") final List<ConvictingInformation> convictingInformations) {
        this.caseId = caseId;
        this.convictingInformations = convictingInformations;

    }

    public UUID getCaseId() {
        return this.caseId;
    }

    public List<ConvictingInformation> getConvictingInformations() {
        return this.convictingInformations;
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
