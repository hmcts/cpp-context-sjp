package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class EnforcementAreaEmailHelperTest {

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
    EnforcementAreaEmailHelper enforcementAreaEmailHelper;

    @Test
    public void shouldReturnEmail() {
        final String postcode = "BACD12";

        when(lastDecisionHelper.getLastDecision(caseDetails)).thenReturn(of(lastDecision));
        when(lastDecision.getSession()).thenReturn(session);
        final String ljaCode = "ljaCode";
        when(session.getLocalJusticeAreaNationalCourtCode()).thenReturn(ljaCode);
        when(enforcementAreaService.getEnforcementArea(postcode, ljaCode, jsonEnvelope)).thenReturn(enforcementArea);
        when(enforcementArea.getString("email")).thenReturn("email");
        final String email = enforcementAreaEmailHelper.enforcementEmail(jsonEnvelope, caseDetails, postcode);
        assertThat(email, is("email"));
    }

    @Test
    public void shouldThrowExceptionWhenEnforcementAreaIsNotAvailable() {
        final String postcode = "BACD12";
        when(lastDecisionHelper.getLastDecision(caseDetails)).thenReturn(of(lastDecision));
        when(lastDecision.getSession()).thenReturn(session);
        final String ljaCode = "ljaCode";
        when(session.getLocalJusticeAreaNationalCourtCode()).thenReturn(ljaCode);
        when(enforcementAreaService.getEnforcementArea(postcode, ljaCode, jsonEnvelope)).thenReturn(enforcementArea);
        when(enforcementArea.getString("email")).thenReturn(null);

        final String errorMessage = String.format("Unable to find Enforcement area email address in reference data for postcode : %s", postcode);
        var e = assertThrows(IllegalStateException.class, () -> enforcementAreaEmailHelper.enforcementEmail(jsonEnvelope, caseDetails, postcode));
        assertThat(e.getMessage(), is(errorMessage));
    }
}