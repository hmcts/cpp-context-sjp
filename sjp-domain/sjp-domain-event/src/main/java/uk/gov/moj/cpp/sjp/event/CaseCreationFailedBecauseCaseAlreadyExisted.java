package uk.gov.moj.cpp.sjp.event;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event("sjp.events.case-creation-failed-because-case-already-existed")
public class CaseCreationFailedBecauseCaseAlreadyExisted {

    private final UUID caseId;

    private final String urn;

    public CaseCreationFailedBecauseCaseAlreadyExisted(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("urn") final String urn) {
        this.caseId = caseId;
        this.urn = urn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
