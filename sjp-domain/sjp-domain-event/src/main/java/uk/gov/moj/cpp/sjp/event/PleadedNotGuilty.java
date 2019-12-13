package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.PleadedNotGuilty.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@Event(EVENT_NAME)
public class PleadedNotGuilty {

    public static final String EVENT_NAME = "sjp.events.pleaded-not-guilty";

    private final UUID caseId;

    private final UUID defendantId;

    private final UUID offenceId;

    private final PleaMethod method;

    private final String notGuiltyBecause;

    private final ZonedDateTime pleadDate;

    @JsonCreator
    public PleadedNotGuilty(@JsonProperty("caseId") final UUID caseId,
                            @JsonProperty("defendantId") final UUID defendantId,
                            @JsonProperty("offenceId") final UUID offenceId,
                            @JsonProperty("notGuiltyBecause") final String notGuiltyBecause,
                            @JsonProperty("pleadDate") final ZonedDateTime pleadDate,
                            @JsonProperty("method") final PleaMethod method) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offenceId = offenceId;
        this.method = method;
        this.pleadDate = pleadDate;
        this.notGuiltyBecause = notGuiltyBecause;
    }

    public PleadedNotGuilty(final UUID caseId,
                            final UUID defendantId,
                            final UUID offenceId,
                            final ZonedDateTime pleadDate,
                            final PleaMethod method) {
        this(caseId, defendantId, offenceId, null, pleadDate, method);
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

    public ZonedDateTime getPleadDate() {
        return pleadDate;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

    @Override
    public String toString() {
        return "PleadedNotGuilty{" +
                "caseId=" + caseId +
                ", defendantId=" + defendantId +
                ", offenceId=" + offenceId +
                ", method=" + method +
                ", notGuiltyBecause='" + notGuiltyBecause + '\'' +
                ", pleadDate=" + pleadDate +
                '}';
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
        final PleadedNotGuilty that = (PleadedNotGuilty) o;
        return Objects.equal(caseId, that.caseId) &&
                Objects.equal(defendantId, that.defendantId) &&
                Objects.equal(offenceId, that.offenceId) &&
                method == that.method &&
                Objects.equal(notGuiltyBecause, that.notGuiltyBecause) &&
                Objects.equal(pleadDate, that.pleadDate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(caseId, defendantId, offenceId, method, notGuiltyBecause, pleadDate);
    }
}
