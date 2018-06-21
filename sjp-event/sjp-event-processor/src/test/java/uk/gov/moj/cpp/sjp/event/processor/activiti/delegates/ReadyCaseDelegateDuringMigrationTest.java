package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalTime.MIDNIGHT;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CREATE_BY_PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.MARKED_READY_TIMESTAMP_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

//TODO ATCM-3133 - remove
@RunWith(MockitoJUnitRunner.class)
public class ReadyCaseDelegateDuringMigrationTest extends AbstractCaseDelegateTest {

    private final ZonedDateTime now = ZonedDateTime.now();

    private final LocalDate postingDate = LocalDate.now().minusDays(30);

    @Spy
    private Clock clock = new StoppedClock(now);

    @InjectMocks
    private ReadyCaseDelegate readyCaseDelegate;

    @Before
    public void setUp() {
        when(delegateExecution.getVariable(PROVED_IN_ABSENCE_VARIABLE, Boolean.class)).thenReturn(true);
        when(delegateExecution.getVariable(POSTING_DATE_VARIABLE, String.class)).thenReturn(postingDate.toString());
        doAnswer(invocation -> when(delegateExecution.getVariable(MARKED_READY_TIMESTAMP_VARIABLE, String.class))
                .thenReturn((String) invocation.getArguments()[1])
        ).when(delegateExecution).setVariable(eq(MARKED_READY_TIMESTAMP_VARIABLE), anyString());
    }

    @Test
    public void shouldCalculateMarkedReadyTimestampBasedOnPostingDateWhenProvedInAbsenceAndProcessStartedByMigration() {
        when(delegateExecution.hasVariable(CREATE_BY_PROCESS_MIGRATION_VARIABLE)).thenReturn(true);

        readyCaseDelegate.execute(delegateExecution);

        final ZonedDateTime expectedMarkedAt = ZonedDateTime.of(postingDate.plusDays(28), MIDNIGHT, UTC);

        verifyCaseMarkedReady(caseId, PIA, expectedMarkedAt);
    }

    @Test
    public void shouldCalculateMarkedReadyTimestampBasedOnCurrentTimeWhenNotProvedInAbsenceAndProcessStartedByMigration() {
        when(delegateExecution.hasVariable(CREATE_BY_PROCESS_MIGRATION_VARIABLE)).thenReturn(true);
        when(delegateExecution.getVariable(WITHDRAWAL_REQUESTED_VARIABLE, Boolean.class)).thenReturn(true);

        readyCaseDelegate.execute(delegateExecution);

        verifyCaseMarkedReady(caseId, WITHDRAWAL_REQUESTED, now);
    }

    @Test
    public void shouldCalculateMarkedReadyTimestampBasedOnCurrentTimeWhenProvedInAbsenceAndProcessNotStartedByMigration() {
        when(delegateExecution.hasVariable(CREATE_BY_PROCESS_MIGRATION_VARIABLE)).thenReturn(false);

        readyCaseDelegate.execute(delegateExecution);

        verifyCaseMarkedReady(caseId, PIA, now);
    }

    private void verifyCaseMarkedReady(final UUID caseId, final CaseReadinessReason readinessReason, final ZonedDateTime markedReadyAt) {
        verify(sender).sendAsAdmin(argThat(
                jsonEnvelope(
                        metadata().of(metadata).withName("sjp.command.mark-case-ready-for-decision"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.reason", equalTo(readinessReason.name())),
                                withJsonPath("$.markedAt", equalTo(markedReadyAt.toString()))))
                )));
    }

}
