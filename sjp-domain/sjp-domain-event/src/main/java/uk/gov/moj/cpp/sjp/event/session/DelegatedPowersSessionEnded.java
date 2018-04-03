package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event("sjp.events.delegated-powers-session-ended")
public class DelegatedPowersSessionEnded extends SessionEnded {

    public DelegatedPowersSessionEnded(final UUID sessionId, final ZonedDateTime endedAt) {
        super(sessionId, endedAt);
    }
}
