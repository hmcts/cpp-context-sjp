package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WithdrawalRequestedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private WithdrawalRequestedDelegate withdrawalRequestedDelegate;

    @BeforeEach
    public void init() {
        caseId = UUID.randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class))
                .thenReturn(metadataToString(metadata));
    }

    @Test
    public void shouldEmitPublicEventAndSetWithdrawalRequestedProcessVariableToTrue() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(false);
        withdrawalRequestedDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.all-offences-withdrawal-requested"),
                        payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))
                )));

        verify(delegateExecution).setVariable(WITHDRAWAL_REQUESTED_VARIABLE, true);
        verify(delegateExecution, never()).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }

    @Test
    public void shouldNotEmitPublicEventDuringMigration() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(true);
        withdrawalRequestedDelegate.execute(delegateExecution);

        verify(sender, never()).send(any());

        verify(delegateExecution).setVariable(WITHDRAWAL_REQUESTED_VARIABLE, true);
        verify(delegateExecution).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }
}
