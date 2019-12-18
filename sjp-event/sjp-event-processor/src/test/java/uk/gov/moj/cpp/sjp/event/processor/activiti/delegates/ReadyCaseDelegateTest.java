package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.processor.activiti.ExpectedDateReadyCalculator;
import uk.gov.moj.cpp.sjp.event.processor.activiti.ReadyCaseCalculator;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReadyCaseDelegateTest extends AbstractCaseDelegateTest {

    @Mock
    private ReadyCaseCalculator readyCaseCalculator;

    @Mock
    private ExpectedDateReadyCalculator expectedDateReadyCalculator;

    @InjectMocks
    private ReadyCaseDelegate readyCaseDelegate;

    @Captor
    private ArgumentCaptor<Boolean> booleanArgumentCaptor;

    @Captor
    private ArgumentCaptor<PleaType> pleaTypeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    @Test
    public void shouldSendUnmarkCaseReadyCommandWhenCaseIsNotReady() {
        final LocalDateTime expectedDateReady = now();
        // GIVEN
        when(readyCaseCalculator.getReasonIfReady(anyBoolean(), anyBoolean(), anyBoolean(), any(PleaType.class), anyBoolean()))
                .thenReturn(Optional.empty());

        when(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution)).thenReturn(expectedDateReady);

        // WHEN
        readyCaseDelegate.execute(delegateExecution);

        // THEN
        verify(sender).sendAsAdmin(argumentCaptor.capture());

        verify(sender).sendAsAdmin(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("sjp.command.unmark-case-ready-for-decision"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.expectedDateReady", equalTo(expectedDateReady.toLocalDate().toString()))
                        )))));

        verifyGetVariableCalls();
        verify(delegateExecution).setVariable("isReady", false);
    }

    @Test
    public void shouldMarkCaseAsReadyWhenThereIsDecision() {
        // GIVEN
        final CaseReadinessReason expectedCaseReadinessReason = CaseReadinessReason.PIA;
        when(readyCaseCalculator.getReasonIfReady(anyBoolean(), anyBoolean(), anyBoolean(), any(PleaType.class), anyBoolean()))
                .thenReturn(Optional.of(expectedCaseReadinessReason));

        // WHEN
        readyCaseDelegate.execute(delegateExecution);

        // THEN
        verify(sender).sendAsAdmin(argumentCaptor.capture());

        verify(sender).sendAsAdmin(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("sjp.command.mark-case-ready-for-decision"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.reason", equalTo(expectedCaseReadinessReason.name())),
                                withJsonPath("$.markedAt", equalTo(clock.now().toString())))))));

        verifyGetVariableCalls();
        verify(expectedDateReadyCalculator, never()).calculateExpectedDateReady(any());
        verify(delegateExecution).setVariable("isReady", true);
        verify(expectedDateReadyCalculator, never()).calculateExpectedDateReady(any());
    }

    @Test
    public void shouldPleaReadyBeBackwardCompatibleWhenPleaReadyNotPresentWithoutPlea() {
        callDelegateWithoutPleaReadyAndWith(null);
    }

    @Test
    public void shouldPleaReadyBeBackwardCompatibleWhenPleaReadyNotPresentWithPlea() {
        callDelegateWithoutPleaReadyAndWith(PleaType.GUILTY);
    }

    private void callDelegateWithoutPleaReadyAndWith(final PleaType pleaType) {
        final LocalDateTime expectedDateReady = now();
        // GIVEN
        when(readyCaseCalculator.getReasonIfReady(anyBoolean(), anyBoolean(), anyBoolean(), any(PleaType.class), anyBoolean()))
                .thenReturn(Optional.empty());
        when(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution)).thenReturn(expectedDateReady);
        when(delegateExecution.getVariable(PLEA_READY_VARIABLE)).thenReturn(null);
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class))
                .thenReturn(Optional.ofNullable(pleaType).map(PleaType::name).orElse(null));

        // WHEN
        readyCaseDelegate.execute(delegateExecution);

        // THEN
        verify(readyCaseCalculator).getReasonIfReady(anyBoolean(), anyBoolean(), booleanArgumentCaptor.capture(), pleaTypeArgumentCaptor.capture(), anyBoolean());

        assertThat(booleanArgumentCaptor.getValue(), equalTo(pleaType != null));
        assertThat(pleaTypeArgumentCaptor.getValue(), equalTo(pleaType));

        verifyGetVariableCalls();
    }

    public void verifyGetVariableCalls() {
        verify(delegateExecution).getVariable(WITHDRAWAL_REQUESTED_VARIABLE, Boolean.class);
        verify(delegateExecution).getVariable(PROVED_IN_ABSENCE_VARIABLE, Boolean.class);
        verify(delegateExecution).getVariable(PLEA_READY_VARIABLE, Boolean.class);
        verify(delegateExecution).getVariable(PLEA_TYPE_VARIABLE, String.class);
    }

}
