package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("squid:S2187")
public class LastDecisionHelperTest extends TestCase {

    @Mock
    private CaseDecision lastDecision;

    @Mock
    private List<CaseDecision> caseDecisions;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private Stream<CaseDecision> streamOfCaseDecisions;

    @InjectMocks
    LastDecisionHelper lastDecisionHelper;

    @Test
    public void shouldReturnLastDecision() {
        when(caseDetails.getCaseDecisions()).thenReturn(caseDecisions);
        when(caseDecisions.get(0)).thenReturn(lastDecision);
        when(lastDecision.getSavedAt()).thenReturn(now());
        when(caseDecisions.stream()).thenReturn(streamOfCaseDecisions);
        final Optional<CaseDecision> lastDecision1 = of(this.lastDecision);
        when(streamOfCaseDecisions.max(any())).thenReturn(lastDecision1);
        final Optional<CaseDecision> caseDecisionOpt = lastDecisionHelper.getLastDecision(caseDetails);
        assertThat(caseDecisionOpt, is(of(lastDecision)));
    }

    @Test
    public void shouldReturnNoLastDecision() {
        when(caseDetails.getCaseDecisions()).thenReturn(new ArrayList());
        when(lastDecision.getSavedAt()).thenReturn(now());
        when(caseDecisions.stream()).thenReturn(streamOfCaseDecisions);
        final Optional<CaseDecision> lastDecision1 = of(this.lastDecision);
        when(streamOfCaseDecisions.max(any())).thenReturn(lastDecision1);
        final Optional<CaseDecision> caseDecisionOpt = lastDecisionHelper.getLastDecision(caseDetails);
        assertThat(caseDecisionOpt, is(empty()));
    }
}