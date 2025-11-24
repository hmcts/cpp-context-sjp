package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event(AocpSessionEnded.EVENT_NAME)
public class AocpSessionEnded extends SessionEnded {

    public static final String EVENT_NAME = "sjp.events.aocp-session-ended";

    public AocpSessionEnded(final UUID sessionId, final ZonedDateTime endedAt) {
        super(sessionId, endedAt);
    }
}
