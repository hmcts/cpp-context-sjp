package uk.gov.moj.cpp.sjp.event;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event(CaseReserveFailedAsAlreadyCompleted.EVENT_NAME)
public class CaseReserveFailedAsAlreadyCompleted {

    public static final String EVENT_NAME = "sjp.events.case-reserve-failed-as-already-completed";

    private final UUID caseId;

    @JsonCreator
    public CaseReserveFailedAsAlreadyCompleted(
            @JsonProperty("caseId") final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
