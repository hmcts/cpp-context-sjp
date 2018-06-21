package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class WithdrawAllOffencesTest extends CaseAggregateBaseTest {

    @Test
    public void shouldWithdrawAllOffences() {
        final List<Object> events = caseAggregate.requestWithdrawalAllOffences().collect(Collectors.toList());

        assertThat(events.size(), is(1));

        final AllOffencesWithdrawalRequested event = (AllOffencesWithdrawalRequested) events.get(0);
        assertEquals(aCase.getId(), event.getCaseId());
    }

    @Test
    public void shouldReturnCaseNotFoundWhenTheCaseIsNotCreated() {

        final Stream<Object> stream = new CaseAggregate().requestWithdrawalAllOffences();

        final Optional<Object> event = stream.findFirst();
        assertTrue(event.isPresent());
        assertThat(event.get().getClass(), equalTo(CaseNotFound.class));
    }
}

