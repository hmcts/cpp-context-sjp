package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.adjournment;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.AbstractCaseDelegateTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAdjournmentElapsedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private CaseAdjournmentElapsedDelegate caseAdjournmentElapsedDelegate;

    @BeforeEach
    public void init() {
        caseId = UUID.randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class))
                .thenReturn(metadataToString(metadata));
    }

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
