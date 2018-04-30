package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event(DelegatedPowersSessionEnded.EVENT_NAME)
public class DelegatedPowersSessionEnded extends SessionEnded {

    public static final String EVENT_NAME = "sjp.events.delegated-powers-session-ended";

    public DelegatedPowersSessionEnded(final UUID sessionId, final ZonedDateTime endedAt) {
        super(sessionId, endedAt);
    }
}
