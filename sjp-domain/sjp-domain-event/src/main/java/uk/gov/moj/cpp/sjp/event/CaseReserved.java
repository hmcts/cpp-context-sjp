package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import uk.gov.justice.domain.annotation.Event;

@Event(CaseReserved.EVENT_NAME)
public class CaseReserved {

    public static final String EVENT_NAME = "sjp.events.case-reserved";

    private final UUID caseId;
    private final String caseUrn;
    private final ZonedDateTime reservedAt;
    private final UUID reservedBy;

    @JsonCreator
    public CaseReserved(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("caseUrn") final String caseUrn,
            @JsonProperty("reservedAt") final ZonedDateTime reservedAt,
            @JsonProperty("reservedBy") final UUID reservedBy) {
        this.caseId = caseId;
        this.caseUrn = caseUrn;
        this.reservedAt = reservedAt;
        this.reservedBy = reservedBy;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public ZonedDateTime getReservedAt() {
        return reservedAt;
    }

    public UUID getReservedBy() {
        return reservedBy;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
