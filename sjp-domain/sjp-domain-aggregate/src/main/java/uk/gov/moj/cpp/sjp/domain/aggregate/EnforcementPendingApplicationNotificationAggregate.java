package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerated;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationQueued;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationSent;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class EnforcementPendingApplicationNotificationAggregate implements Aggregate {

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(EnforcementPendingApplicationNotificationGenerated.class).apply(e -> doNothing()),
                when(EnforcementPendingApplicationNotificationGenerationFailed.class).apply(e -> doNothing()),
                when(EnforcementPendingApplicationNotificationQueued.class).apply(e -> doNothing()),
                when(EnforcementPendingApplicationNotificationFailed.class).apply(e -> doNothing()),
                when(EnforcementPendingApplicationNotificationSent.class).apply(e -> doNothing()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> markAsGenerated( final UUID applicationId, final UUID fileId, final ZonedDateTime generatedTime) {
        return apply(of(new EnforcementPendingApplicationNotificationGenerated(applicationId, fileId, generatedTime)));
    }

    public Stream<Object> markAsGenerationFailed(final UUID applicationId, final ZonedDateTime generationFailedTime) {
        return apply(of(new EnforcementPendingApplicationNotificationGenerationFailed(applicationId, generationFailedTime)));
    }

    public Stream<Object> markAsNotificationQueued(final UUID applicationId, final ZonedDateTime queuedTime) {
        return apply(of(new EnforcementPendingApplicationNotificationQueued(applicationId, queuedTime)));
    }

    public Stream<Object> markAsNotificationFailed(final UUID applicationId, final ZonedDateTime failedTime) {
        return apply(of(new EnforcementPendingApplicationNotificationFailed(applicationId, failedTime)));
    }

    public Stream<Object> markAsNotificationSent(final UUID applicationId, final ZonedDateTime sentTime) {
        return apply(of(new EnforcementPendingApplicationNotificationSent(applicationId, sentTime)));
    }
}
