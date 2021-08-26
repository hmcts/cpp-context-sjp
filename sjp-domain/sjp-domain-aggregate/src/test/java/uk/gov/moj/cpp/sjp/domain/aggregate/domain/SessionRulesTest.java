package uk.gov.moj.cpp.sjp.domain.aggregate.domain;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.DEFAULT_STATUS;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.UNKNOWN;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.Priority.HIGH;
import static uk.gov.moj.cpp.sjp.domain.Priority.LOW;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getPriority;
import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getSessionType;

@RunWith(MockitoJUnitRunner.class)
public class SessionRulesTest {

    private static final boolean ADJOURNED_POST_CONVICTION = true;
    private static final boolean NOT_ADJOURNED_POST_CONVICTION = false;
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

    @Test
    public void shouldGetMagistrateSession() {
        assertThat(getSessionType(PIA, NOT_ADJOURNED_POST_CONVICTION, false,false), is(MAGISTRATE));
        assertThat(getSessionType(PLEADED_GUILTY, NOT_ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(PIA, ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(PLEADED_GUILTY, ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(PLEADED_NOT_GUILTY, ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(PLEADED_GUILTY_REQUEST_HEARING, ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(WITHDRAWAL_REQUESTED, ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(UNKNOWN, ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(DEFAULT_STATUS, ADJOURNED_POST_CONVICTION,false,false), is(MAGISTRATE));
        assertThat(getSessionType(DEFAULT_STATUS, NOT_ADJOURNED_POST_CONVICTION, true,false), is(MAGISTRATE));
        assertThat(getSessionType(DEFAULT_STATUS, NOT_ADJOURNED_POST_CONVICTION, true,true), is(MAGISTRATE));

    }

    @Test
    public void shouldGetDelegatedPowersSessionType() {
        assertThat(getSessionType(PLEADED_NOT_GUILTY, NOT_ADJOURNED_POST_CONVICTION,false,false), is(DELEGATED_POWERS));
        assertThat(getSessionType(PLEADED_GUILTY_REQUEST_HEARING, NOT_ADJOURNED_POST_CONVICTION,false,false), is(DELEGATED_POWERS));
        assertThat(getSessionType(WITHDRAWAL_REQUESTED, NOT_ADJOURNED_POST_CONVICTION,false,false), is(DELEGATED_POWERS));
        assertThat(getSessionType(UNKNOWN, NOT_ADJOURNED_POST_CONVICTION,false,false), is(DELEGATED_POWERS));
        assertThat(getSessionType(DEFAULT_STATUS, NOT_ADJOURNED_POST_CONVICTION,false,false), is(DELEGATED_POWERS));
    }

    @Test
    public void shouldResolvePriorityToHighWhenTheDecisionIsSetAside() {
        when(caseAggregateState.isSetAside()).thenReturn(true);
        assertThat(getPriority(caseAggregateState), is(HIGH));
    }

}