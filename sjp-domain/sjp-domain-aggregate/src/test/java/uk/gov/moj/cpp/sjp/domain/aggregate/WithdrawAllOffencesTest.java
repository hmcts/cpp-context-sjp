package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class WithdrawAllOffencesTest extends CaseAggregateBaseTest {

    @Test
    public void shouldWithdrawAllOffences() {
        final List<Object> events = caseAggregate.requestWithdrawalAllOffences(UUID.randomUUID().toString()).collect(Collectors.toList());

        assertThat(events.size(), is(1));

        final AllOffencesWithdrawalRequested event = (AllOffencesWithdrawalRequested) events.get(0);
        assertEquals(aCase.getId(), event.getCaseId());
    }

    @Test
    public void shouldReturnCaseNotFoundWhenTheCaseIsNotCreated() {

        final Stream<Object> stream = new CaseAggregate().requestWithdrawalAllOffences(UUID.randomUUID().toString());

        assertThat(stream.count(), is(1L));
    }
}

