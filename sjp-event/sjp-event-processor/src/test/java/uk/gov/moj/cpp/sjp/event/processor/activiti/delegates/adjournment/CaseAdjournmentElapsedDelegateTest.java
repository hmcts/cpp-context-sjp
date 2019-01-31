package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.adjournment;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;

import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.AbstractCaseDelegateTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAdjournmentElapsedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private CaseAdjournmentElapsedDelegate caseAdjournmentElapsedDelegate;

    @Test
    public void shouldResetAdjournmentVariableAndSendAdjournmentElapsedCommand() {
        caseAdjournmentElapsedDelegate.execute(delegateExecution);

        verify(delegateExecution).setVariable(CASE_ADJOURNED_VARIABLE, false);

        verify(sender).sendAsAdmin(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("sjp.command.record-case-adjournment-to-later-sjp-hearing-elapsed"),
                        payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))
                )));
    }
}
