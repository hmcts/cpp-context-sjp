package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event("sjp.events.magistrate-session-ended")
public class MagistrateSessionEnded extends SessionEnded {

    public MagistrateSessionEnded(final UUID sessionId, final ZonedDateTime endedAt) {
        super(sessionId, endedAt);
    }
}
