package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
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
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleaCancelledDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private PleaCancelledDelegate pleaCancelledDelegate;

    private final UUID offenceId = randomUUID();

    @BeforeEach
    public void setUp() {
        caseId = UUID.randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class))
                .thenReturn(metadataToString(metadata));
    }

    @Test
    public void shouldEmitPublicEventAndRemovePleaTypeProcessVariables() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(false);
        when(delegateExecution.getVariable(OFFENCE_ID_VARIABLE, String.class)).thenReturn(offenceId.toString());

        pleaCancelledDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.plea-cancelled"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.offenceId", equalTo(offenceId.toString()))
                        ))
                )));

        verify(delegateExecution, never()).removeVariable(PROCESS_MIGRATION_VARIABLE);
        verify(delegateExecution).removeVariables(asList(PLEA_TYPE_VARIABLE, PLEA_READY_VARIABLE));
    }

    @Test
    public void shouldNotEmitPublicEventDuringMigration() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(true);

        pleaCancelledDelegate.execute(delegateExecution);

        verify(sender, never()).send(any());

        verify(delegateExecution).removeVariables(asList(PLEA_TYPE_VARIABLE, PLEA_READY_VARIABLE));
    }
}
