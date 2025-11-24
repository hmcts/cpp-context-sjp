package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorFailed;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorQueued;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorSent;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class PartialAocpCriteriaNotificationAggregate implements Aggregate {
    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(PartialAocpCriteriaNotificationProsecutorQueued.class).apply(e -> doNothing()),
                when(PartialAocpCriteriaNotificationProsecutorFailed.class).apply(e -> doNothing()),
                when(PartialAocpCriteriaNotificationProsecutorSent.class).apply(e -> doNothing()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> markAsNotificationQueued(final UUID applicationDecisionId) {
        return apply(of(new PartialAocpCriteriaNotificationProsecutorQueued(applicationDecisionId)));
    }

    public Stream<Object> markAsNotificationFailed(final UUID applicationDecisionId, final ZonedDateTime failedTime) {
        return apply(of(new PartialAocpCriteriaNotificationProsecutorFailed(applicationDecisionId, failedTime)));
    }

    public Stream<Object> markAsNotificationSent(final UUID applicationDecisionId, final ZonedDateTime sentTime) {
        return apply(of(new PartialAocpCriteriaNotificationProsecutorSent(applicationDecisionId, sentTime)));
    }
}
