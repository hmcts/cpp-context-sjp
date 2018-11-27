package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaUpdatedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private PleaUpdatedDelegate pleaUpdatedDelegate;

    @Test
    public void shouldEmitPublicEventAndSetPleaTypeAsProcessVariables() {
        final UUID offenceId = randomUUID();
        final PleaType pleaType = GUILTY;

        when(delegateExecution.hasVariable(PLEA_TYPE_VARIABLE)).thenReturn(true);
        when(delegateExecution.getVariable(OFFENCE_ID_VARIABLE, String.class)).thenReturn(offenceId.toString());
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class)).thenReturn(pleaType.name());

        pleaUpdatedDelegate.execute(delegateExecution);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("public.sjp.plea-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.offenceId", equalTo(offenceId.toString())),
                                withJsonPath("$.plea", equalTo(pleaType.name()))
                        ))
                )));

        verify(delegateExecution).setVariable(PLEA_READY_VARIABLE, true);
    }

    @Test
    public void shouldSetPleaNotReadyWhenPleaNotNotGuilty() {
        final PleaType pleaType = NOT_GUILTY;

        when(delegateExecution.hasVariable(PLEA_TYPE_VARIABLE)).thenReturn(true);
        when(delegateExecution.getVariable(OFFENCE_ID_VARIABLE, String.class)).thenReturn(randomUUID().toString());
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class)).thenReturn(pleaType.name());

        pleaUpdatedDelegate.execute(delegateExecution);

        verify(delegateExecution).setVariable(PLEA_READY_VARIABLE, false);
    }

    @Test
    public void shouldCalculatePleaReady() {
        for (final PleaType pleaType : PleaType.values()) {
            asList(TRUE, FALSE)
                    .forEach(areDatesToAvoidSet -> {
                        boolean expectedPleaReady = ! pleaType.equals(NOT_GUILTY) || areDatesToAvoidSet;
                        assertThat(
                                format("Assertion Error: PleaUpdatedDelegate.isPleaReady(%s, %s) != %s", pleaType, areDatesToAvoidSet, expectedPleaReady),
                                PleaUpdatedDelegate.isPleaReady(pleaType, areDatesToAvoidSet), equalTo(expectedPleaReady));
                    });
        }
    }

}
