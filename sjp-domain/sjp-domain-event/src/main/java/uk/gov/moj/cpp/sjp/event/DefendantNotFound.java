package uk.gov.moj.cpp.sjp.event;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(DefendantNotFound.EVENT_NAME)
public class DefendantNotFound {

    public static final String EVENT_NAME = "sjp.events.defendant-not-found";

    private final UUID defendantId;
    private final String description;

    @JsonCreator
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
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
