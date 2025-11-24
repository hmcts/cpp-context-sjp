package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(ResetAocpSession.EVENT_NAME)
public class ResetAocpSession {
    
    public static final String EVENT_NAME = "sjp.events.reset-aocp-session-requested";

    private final ZonedDateTime resetAt;

    @JsonCreator
    public ResetAocpSession(@JsonProperty("resetAt") ZonedDateTime resetAt) {
       this.resetAt = resetAt;
    }

    public ZonedDateTime getResetAt() {
        return resetAt;
    }
}
