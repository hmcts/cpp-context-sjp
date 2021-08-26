package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("squid:S2187")
public class DivisionCodeHelperTest extends TestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private LastDecisionHelper lastDecisionHelper;

    @Mock
    private JsonObject enforcementArea;

    @Mock
    private Session session;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private CaseDecision lastDecision;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private EnforcementAreaService enforcementAreaService;

    @InjectMocks
    DivisionCodeHelper divisionCodeHelper;

    @Test
    public void shouldReturnDivisionCode() {
        final String postcode = "BACD12";

        when(lastDecisionHelper.getLastDecision(caseDetails)).thenReturn(of(lastDecision));
        when(lastDecision.getSession()).thenReturn(session);
        final String ljaCode = "ljaCode";
        when(session.getLocalJusticeAreaNationalCourtCode()).thenReturn(ljaCode);
        when(enforcementAreaService.getEnforcementArea(postcode, ljaCode, jsonEnvelope)).thenReturn(enforcementArea);
        when(enforcementArea.getInt("accountDivisionCode")).thenReturn(10);
        final int divisionCode = divisionCodeHelper.divisionCode(jsonEnvelope, caseDetails, postcode);
        assertThat(divisionCode, is(10));
    }

    @Test
    public void shouldThrowExceptionWhenEnforcementAreaIsNotAvailable() {
        final String postcode = "BACD12";
        when(lastDecisionHelper.getLastDecision(caseDetails)).thenReturn(of(lastDecision));
        when(lastDecision.getSession()).thenReturn(session);
        final String ljaCode = "ljaCode";
        when(session.getLocalJusticeAreaNationalCourtCode()).thenReturn(ljaCode);
        when(enforcementAreaService.getEnforcementArea(postcode, ljaCode, jsonEnvelope)).thenReturn(enforcementArea);

        final String errorMessage = String.format("Unable to find Enforcement area division code in reference data for postcode : %s", postcode);
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(errorMessage);
        divisionCodeHelper.divisionCode(jsonEnvelope, caseDetails, postcode);
    }
}