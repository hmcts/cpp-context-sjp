package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.IS_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.MARKED_READY_TIMESTAMP_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(Parameterized.class)
public class ReadyCaseDelegateTest extends AbstractCaseDelegateTest  {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Spy
    private Clock clock = new StoppedClock(now);

    @InjectMocks
    private ReadyCaseDelegate readyCaseDelegate;

    @Parameterized.Parameter(0)
    public boolean withdrawalRequested;

    @Parameterized.Parameter(1)
    public boolean provedInAbsence;

    @Parameterized.Parameter(2)
    public PleaType pleaType;

    @Parameterized.Parameter(3)
    public boolean readyBefore;

    @Parameterized.Parameter(4)
    public CaseReadinessReason expectedReadinessReason;

    @Parameterized.Parameter(5)
    public ZonedDateTime expectedMarkedReadyAt;

    final static ZonedDateTime previousMarkedReadyAt = ZonedDateTime.now().minusHours(1);
    final static ZonedDateTime now = ZonedDateTime.now();

    @Parameterized.Parameters(name = "withdrawal={0} pia={1} plea={2} ready before={3} readiness reason={4}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true, false, null, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, false, null, false, WITHDRAWAL_REQUESTED, now},
                {true, false, GUILTY, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, false, GUILTY, false, WITHDRAWAL_REQUESTED, now},
                {true, false, NOT_GUILTY, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, false, NOT_GUILTY, false, WITHDRAWAL_REQUESTED, now},
                {true, false, GUILTY_REQUEST_HEARING, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, false, GUILTY_REQUEST_HEARING, false, WITHDRAWAL_REQUESTED, now},
                {true, true, null, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, true, null, false, WITHDRAWAL_REQUESTED, now},
                {true, true, GUILTY, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, true, GUILTY, false, WITHDRAWAL_REQUESTED, now},
                {true, true, NOT_GUILTY, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, true, NOT_GUILTY, false, WITHDRAWAL_REQUESTED, now},
                {true, true, GUILTY_REQUEST_HEARING, true, WITHDRAWAL_REQUESTED, previousMarkedReadyAt},
                {true, true, GUILTY_REQUEST_HEARING, false, WITHDRAWAL_REQUESTED, now},
                {false, false, null, false, null, null},
                {false, false, GUILTY, true, PLEADED_GUILTY, previousMarkedReadyAt},
                {false, false, GUILTY, false, PLEADED_GUILTY, now},
                {false, false, NOT_GUILTY, true, PLEADED_NOT_GUILTY, previousMarkedReadyAt},
                {false, false, NOT_GUILTY, false, PLEADED_NOT_GUILTY, now},
                {false, false, GUILTY_REQUEST_HEARING, true, PLEADED_GUILTY_REQUEST_HEARING, previousMarkedReadyAt},
                {false, false, GUILTY_REQUEST_HEARING, false, PLEADED_GUILTY_REQUEST_HEARING, now},
                {false, true, null, true, PIA, previousMarkedReadyAt},
                {false, true, null, false, PIA, now},
                {false, true, GUILTY, true, PLEADED_GUILTY, previousMarkedReadyAt},
                {false, true, GUILTY, false, PLEADED_GUILTY, now},
                {false, true, NOT_GUILTY, true, PLEADED_NOT_GUILTY, previousMarkedReadyAt},
                {false, true, NOT_GUILTY, false, PLEADED_NOT_GUILTY, now},
                {false, true, GUILTY_REQUEST_HEARING, true, PLEADED_GUILTY_REQUEST_HEARING, previousMarkedReadyAt},
                {false, true, GUILTY_REQUEST_HEARING, false, PLEADED_GUILTY_REQUEST_HEARING, now}
        });
    }

    @Test
    public void shouldIssueCommandAndSetProcessVariables() {

        when(delegateExecution.hasVariable(CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE)).thenReturn(true);
        when(delegateExecution.getVariable(CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE, Boolean.class)).thenReturn(withdrawalRequested);
        when(delegateExecution.hasVariable(PROVED_IN_ABSENCE_VARIABLE)).thenReturn(true);
        when(delegateExecution.getVariable(PROVED_IN_ABSENCE_VARIABLE, Boolean.class)).thenReturn(provedInAbsence);
        when(delegateExecution.hasVariable(PLEA_TYPE_VARIABLE)).thenReturn(pleaType != null);
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class)).thenReturn(pleaType != null ? pleaType.name() : null);
        when(delegateExecution.getVariable(MARKED_READY_TIMESTAMP_VARIABLE, String.class)).thenReturn(readyBefore ? previousMarkedReadyAt.toString() : null);

        readyCaseDelegate.execute(delegateExecution);

        if (expectedReadinessReason != null) {
            verify(delegateExecution).setVariable(IS_READY_VARIABLE, true);
            verify(delegateExecution, times(readyBefore ? 0 : 1)).setVariable(MARKED_READY_TIMESTAMP_VARIABLE, clock.now().toString());

            verify(sender).sendAsAdmin(argThat(
                    jsonEnvelope(
                            metadata().of(metadata).withName("sjp.command.mark-case-ready-for-decision"),
                            payloadIsJson(allOf(
                                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                                    withJsonPath("$.reason", equalTo(expectedReadinessReason.name())),
                                    withJsonPath("$.markedAt", equalTo(expectedMarkedReadyAt.toString()))))
                    )));
        } else {
            verify(delegateExecution).setVariable(IS_READY_VARIABLE, false);
            verify(delegateExecution).removeVariable(MARKED_READY_TIMESTAMP_VARIABLE);

            verify(sender).sendAsAdmin(argThat(
                    jsonEnvelope(
                            metadata().of(metadata).withName("sjp.command.unmark-case-ready-for-decision"),
                            payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))
                    )));
        }
    }

}
