package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
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

        withdrawalRequestedCancelledDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.all-offences-withdrawal-request-cancelled"),
                        payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))
                )));

        verify(delegateExecution).setVariable(WITHDRAWAL_REQUESTED_VARIABLE, false);
    }
}
