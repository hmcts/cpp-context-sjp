package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Test;

public class CancelWithdrawAllOffencesTest extends CaseAggregateBaseTest {

    @Test
    public void shouldCancelWithdrawAllOffences() {
        final List<Object> events = caseAggregate.cancelRequestWithdrawalAllOffences(UUID.randomUUID()).collect(toList());

        assertThat(events.size(), is(1));

        final AllOffencesWithdrawalRequestCancelled event = (AllOffencesWithdrawalRequestCancelled) events.get(0);
        assertEquals(aCase.getId(), event.getCaseId());
        assertThat("Case withdrawl all offences cancelled", caseAggregate.isWithdrawalAllOffencesRequested(), Matchers.is(false));
    }

    @Test
    public void shouldReturnCaseNotFoundWhenTheCaseIsNotCreated() {

        final Stream<Object> stream = new CaseAggregate().cancelRequestWithdrawalAllOffences(UUID.randomUUID());

        assertThat(stream.count(), is(1L));
    }
}

