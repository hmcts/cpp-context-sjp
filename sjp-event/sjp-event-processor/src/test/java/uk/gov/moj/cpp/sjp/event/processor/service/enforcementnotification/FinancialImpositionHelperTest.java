package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryFinancialImposition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class FinancialImpositionHelperTest {
    @Mock
    private LastDecisionHelper lastDecisionHelper;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private CaseDecision lastDecision;

    @Mock
    private QueryFinancialImposition queryFinancialImposition;

    @InjectMocks
    FinancialImpositionHelper financialImpositionHelper;

    @Test
    public void shouldReturnTrueWhenWeHaveFinancialImposition() {
        when(lastDecisionHelper.getLastDecision(caseDetails)).thenReturn(of(lastDecision));
        when(lastDecision.getFinancialImposition()).thenReturn(queryFinancialImposition);
        final boolean financialImposition = financialImpositionHelper.financialImposition( caseDetails);
        assertThat(financialImposition, is(true));
    }

    @Test
    public void shouldReturnFalseWhenWeHaveFinancialImposition() {
        when(lastDecisionHelper.getLastDecision(caseDetails)).thenReturn(of(lastDecision));
        when(lastDecision.getFinancialImposition()).thenReturn(null);
        final boolean financialImposition = financialImpositionHelper.financialImposition( caseDetails);
        assertThat(financialImposition, is(false));
    }
}