package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;


import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationFailed;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationQueued;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationSent;

public class AocpAcceptedEmailNotificationAggregate implements Aggregate {

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(AocpAcceptedEmailNotificationQueued.class).apply(e -> doNothing()),
                when(AocpAcceptedEmailNotificationFailed.class).apply(e -> doNothing()),
                when(AocpAcceptedEmailNotificationSent.class).apply(e -> doNothing()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> markAsNotificationQueued(final UUID caseId, final ZonedDateTime queuedTime) {
        return apply(of(new AocpAcceptedEmailNotificationQueued(caseId, queuedTime)));
    }

    public Stream<Object> markAsNotificationFailed(final UUID caseId, final ZonedDateTime failedTime) {
        return apply(of(new AocpAcceptedEmailNotificationFailed(caseId, failedTime)));
    }

    public Stream<Object> markAsNotificationSent(final UUID caseId, final ZonedDateTime sentTime) {
        return apply(of(new AocpAcceptedEmailNotificationSent(caseId, sentTime)));
    }
}
