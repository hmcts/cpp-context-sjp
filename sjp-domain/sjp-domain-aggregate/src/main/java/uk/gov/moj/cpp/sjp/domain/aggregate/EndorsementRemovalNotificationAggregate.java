package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerated;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerationFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsQueued;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsSent;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class EndorsementRemovalNotificationAggregate implements Aggregate {

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(NotificationToRemoveEndorsementsGenerated.class).apply(e -> doNothing()),
                when(NotificationToRemoveEndorsementsGenerationFailed.class).apply(e -> doNothing()),
                when(NotificationToRemoveEndorsementsQueued.class).apply(e -> doNothing()),
                when(NotificationToRemoveEndorsementsFailed.class).apply(e -> doNothing()),
                when(NotificationToRemoveEndorsementsSent.class).apply(e -> doNothing()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> markAsGenerated(final UUID applicationDecisionId, final UUID fileId) {
        return apply(of(new NotificationToRemoveEndorsementsGenerated(applicationDecisionId, fileId)));
    }

    public Stream<Object> markAsGenerationFailed(final UUID applicationDecisionId) {
        return apply(of(new NotificationToRemoveEndorsementsGenerationFailed(applicationDecisionId)));
    }

    public Stream<Object> markAsNotificationQueued(final UUID applicationDecisionId) {
        return apply(of(new NotificationToRemoveEndorsementsQueued(applicationDecisionId)));
    }

    public Stream<Object> markAsNotificationFailed(final UUID applicationDecisionId, final ZonedDateTime failedTime) {
        return apply(of(new NotificationToRemoveEndorsementsFailed(applicationDecisionId, failedTime)));
    }

    public Stream<Object> markAsNotificationSent(final UUID applicationDecisionId, final ZonedDateTime sentTime) {
        return apply(of(new NotificationToRemoveEndorsementsSent(applicationDecisionId, sentTime)));
    }
}
