package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CREATE_BY_PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_MIGRATION_VARIABLE;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseStartedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private CaseStartedDelegate caseStartedDelegate;

    private final LocalDate postingDate = LocalDate.now();

    @Before
    public void setUp() {
        when(delegateExecution.getVariable(POSTING_DATE_VARIABLE, String.class)).thenReturn(postingDate.format(ISO_DATE));
    }

    @Test
    public void shouldEmitPublicEvent() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(false);

        caseStartedDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.sjp-case-created"),
                        payloadIsJson(allOf(
                                withJsonPath("$.id", equalTo(caseId.toString())),
                                withJsonPath("$.postingDate", equalTo(postingDate.format(ISO_DATE)))
                        ))
                )));

        verify(delegateExecution, never()).setVariable(eq(CREATE_BY_PROCESS_MIGRATION_VARIABLE), any());
        verify(delegateExecution, never()).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }

    @Test
    public void shouldNotEmitPublicEventDuringMigrationAndSetCreatedByProcessMigrationVariable() {
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(true);

        caseStartedDelegate.execute(delegateExecution);

        verify(sender, never()).send(any());
        verify(delegateExecution).setVariable(CREATE_BY_PROCESS_MIGRATION_VARIABLE, true);
        verify(delegateExecution).removeVariable(PROCESS_MIGRATION_VARIABLE);
    }
}
