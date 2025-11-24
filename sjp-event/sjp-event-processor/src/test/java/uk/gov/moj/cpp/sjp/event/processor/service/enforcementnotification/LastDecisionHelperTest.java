package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class LastDecisionHelperTest {

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
        when(caseDecisions.stream()).thenReturn(streamOfCaseDecisions);
        final Optional<CaseDecision> lastDecision1 = of(this.lastDecision);
        when(streamOfCaseDecisions.max(any())).thenReturn(lastDecision1);
        final Optional<CaseDecision> caseDecisionOpt = lastDecisionHelper.getLastDecision(caseDetails);
        assertThat(caseDecisionOpt, is(of(lastDecision)));
    }

    @Test
    public void shouldReturnNoLastDecision() {
        when(caseDetails.getCaseDecisions()).thenReturn(new ArrayList());
        final Optional<CaseDecision> lastDecision1 = of(this.lastDecision);
        final Optional<CaseDecision> caseDecisionOpt = lastDecisionHelper.getLastDecision(caseDetails);
        assertThat(caseDecisionOpt, is(empty()));
    }
}