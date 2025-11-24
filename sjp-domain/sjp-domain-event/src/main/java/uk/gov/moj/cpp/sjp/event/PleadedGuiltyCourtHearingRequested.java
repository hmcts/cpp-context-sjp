package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import static uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested.EVENT_NAME;

@Event(EVENT_NAME)
public class PleadedGuiltyCourtHearingRequested {
    public static final String EVENT_NAME = "sjp.events.pleaded-guilty-court-hearing-requested";

    private final UUID caseId;

    private final UUID defendantId;

    private final UUID offenceId;

    private final PleaMethod method;

    private final String mitigation;

    private final ZonedDateTime pleadDate;

    @JsonCreator
    public PleadedGuiltyCourtHearingRequested(@JsonProperty("caseId") final UUID caseId,
                                              @JsonProperty("defendantId") final UUID defendantId,
                                              @JsonProperty("offenceId") final UUID offenceId,
                                              @JsonProperty("method") final PleaMethod method,
                                              @JsonProperty("mitigation") final String mitigation,
                                              @JsonProperty("pleadDate") final ZonedDateTime pleadDate) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offenceId = offenceId;
        this.method = method;
        this.mitigation = mitigation;
        this.pleadDate = pleadDate;
    }

    public PleadedGuiltyCourtHearingRequested(final UUID caseId,
                                              final UUID defendantId,
                                              final UUID offenceId,
                                              final PleaMethod method,
                                              final ZonedDateTime pleadDate) {
        this(caseId, defendantId, offenceId, method, null, pleadDate);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public PleaMethod getMethod() {
        return method;
    }

    public String getMitigation() {
        return mitigation;
    }

    public ZonedDateTime getPleadDate() {
        return pleadDate;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PleadedGuiltyCourtHearingRequested that = (PleadedGuiltyCourtHearingRequested) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(offenceId, that.offenceId) &&
                method == that.method &&
                Objects.equals(mitigation, that.mitigation) &&
                Objects.equals(pleadDate, that.pleadDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantId, offenceId, method, mitigation, pleadDate);
    }
}
