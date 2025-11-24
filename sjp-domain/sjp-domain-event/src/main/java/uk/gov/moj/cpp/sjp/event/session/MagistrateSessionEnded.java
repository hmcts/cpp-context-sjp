package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event(MagistrateSessionEnded.EVENT_NAME)
public class MagistrateSessionEnded extends SessionEnded {

    public static final String EVENT_NAME = "sjp.events.magistrate-session-ended";

    public MagistrateSessionEnded(final UUID sessionId, final ZonedDateTime endedAt) {
        super(sessionId, endedAt);
    }
}
