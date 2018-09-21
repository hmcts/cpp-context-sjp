package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Test;

public class CancelWithdrawAllOffencesTest extends CaseAggregateBaseTest {

    @Test
    public void shouldCancelWithdrawAllOffences() {
        caseAggregate.requestWithdrawalAllOffences();
        final List<Object> events = caseAggregate.cancelRequestWithdrawalAllOffences().collect(toList());

        assertThat(events.size(), is(1));

        final AllOffencesWithdrawalRequestCancelled event = (AllOffencesWithdrawalRequestCancelled) events.get(0);
        assertEquals(aCase.getId(), event.getCaseId());
        assertThat("Case withdrawal all offences cancelled", caseAggregate.isWithdrawalAllOffencesRequested(), Matchers.is(false));
    }

    @Test
    public void shouldReturnCaseNotFoundWhenTheCaseIsNotCreated() {

        final Stream<Object> stream = new CaseAggregate().cancelRequestWithdrawalAllOffences();

        final Optional<Object> event = stream.findFirst();
        assertTrue(event.isPresent());
        assertThat(event.get().getClass(), equalTo(CaseNotFound.class));
    }
}

