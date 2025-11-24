package uk.gov.moj.cpp.sjp.event;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(CaseExpectedDateReadyChanged.EVENT_NAME)
public class CaseExpectedDateReadyChanged {

    public static final String EVENT_NAME = "sjp.events.case-expected-date-ready-changed";

    private final UUID caseId;
    private final LocalDate oldExpectedDateReady;
    private final LocalDate newExpectedDateReady;

    @JsonCreator
    public CaseExpectedDateReadyChanged(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("oldExpectedDateReady") final LocalDate oldExpectedDateReady,
            @JsonProperty("newExpectedDateReady") final LocalDate newExpectedDateReady) {
        this.caseId = caseId;
        this.oldExpectedDateReady = oldExpectedDateReady;
        this.newExpectedDateReady = newExpectedDateReady;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getOldExpectedDateReady() {
        return oldExpectedDateReady;
    }

    public LocalDate getNewExpectedDateReady() {
        return newExpectedDateReady;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
