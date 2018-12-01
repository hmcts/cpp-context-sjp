package uk.gov.moj.cpp.sjp.event;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(DefendantNotFound.EVENT_NAME)
public class DefendantNotFound {

    public static final String EVENT_NAME = "sjp.events.defendant-not-found";

    private final UUID defendantId;
    private final String description;

    public DefendantNotFound(final UUID defendantId, final String description) {
        this.defendantId = defendantId;
        this.description = description;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefendantNotFound)) {
            return false;
        }
        final DefendantNotFound that = (DefendantNotFound) o;

        return Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantId, description);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
