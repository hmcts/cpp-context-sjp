package uk.gov.moj.cpp.sjp.domain.aggregate.domain;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.Priority.HIGH;
import static uk.gov.moj.cpp.sjp.domain.Priority.LOW;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;
import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getPriority;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;

import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionRulesTest {

    @Mock
    private CaseAggregateState caseAggregateState;

    @Test
    public void shouldResolvePriorityToHighWhenTheWithdrawalIsRequestedOnAllOffences() {
        when(caseAggregateState.withdrawalRequestedOnAllOffences()).thenReturn(true);
        when(caseAggregateState.getOffenceIdsWithPleas()).thenReturn(Sets.newHashSet(UUID.randomUUID()));

        assertThat(getPriority(caseAggregateState), is(HIGH));
    }


    @Test
    public void shouldResolvePriorityToMediumWhenTheWithdrawalIsNotRequestedOnAllOffencesAndPleaOnOneCase() {
        when(caseAggregateState.withdrawalRequestedOnAllOffences()).thenReturn(false);
        when(caseAggregateState.getOffenceIdsWithPleas()).thenReturn(Sets.newHashSet(UUID.randomUUID()));

        assertThat(getPriority(caseAggregateState), is(MEDIUM));
    }


    @Test
    public void shouldResolvePriorityToLowWhenTheWithdrawalIsNotRequestedOnAllOffencesAndNotAtleastOnePlea() {
        when(caseAggregateState.withdrawalRequestedOnAllOffences()).thenReturn(false);
        when(caseAggregateState.getOffenceIdsWithPleas()).thenReturn(Sets.newHashSet());

        assertThat(getPriority(caseAggregateState), is(LOW));
    }



}