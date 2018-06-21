package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WithdrawalRequestCancelledDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private WithdrawalRequestCancelledDelegate withdrawalRequestedCancelledDelegate;

    @Test
    public void shouldEmitPublicEventAndSetWithdrawalRequestedProcessVariableToFalse() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(false);

        withdrawalRequestedCancelledDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.all-offences-withdrawal-request-cancelled"),
                        payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))
                )));

        verify(delegateExecution).setVariable(WITHDRAWAL_REQUESTED_VARIABLE, false);
        verify(delegateExecution, never()).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }

    @Test
    public void shouldNotEmitPublicEventDuringMigration() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(true);

        withdrawalRequestedCancelledDelegate.execute(delegateExecution);

        verify(sender, never()).send(any());

        verify(delegateExecution).setVariable(WITHDRAWAL_REQUESTED_VARIABLE, false);
        verify(delegateExecution).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }

}
