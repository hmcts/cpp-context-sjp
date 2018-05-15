package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_MIGRATION_VARIABLE;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaCancelledDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private PleaCancelledDelegate pleaCancelledDelegate;

    private final UUID offenceId = randomUUID();

    @Before
    public void setUp() {
        when(delegateExecution.getVariable(OFFENCE_ID_VARIABLE, String.class)).thenReturn(offenceId.toString());
    }

    @Test
    public void shouldEmitPublicEventAndRemovePleaTypeProcessVariables() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(false);

        pleaCancelledDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.plea-cancelled"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.offenceId", equalTo(offenceId.toString()))
                        ))
                )));

        verify(delegateExecution).removeVariable(PLEA_TYPE_VARIABLE);
        verify(delegateExecution, never()).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }

    @Test
    public void shouldNotEmitPublicEventDuringMigration() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(true);

        pleaCancelledDelegate.execute(delegateExecution);

        verify(sender, never()).send(any());

        verify(delegateExecution).removeVariable(PLEA_TYPE_VARIABLE);
        verify(delegateExecution).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }
}
