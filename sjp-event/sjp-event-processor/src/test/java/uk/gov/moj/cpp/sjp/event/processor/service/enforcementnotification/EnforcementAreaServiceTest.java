package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class EnforcementAreaServiceTest {

    private final String postcode = "CR0 2GE";
    private final String ljaNationalCourtCode = "255";

    @Mock
    private JsonObject enforcementArea1;

    @Mock
    private JsonObject enforcementArea2;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private EnforcementAreaService enforcementAreaService = new EnforcementAreaService();

    @Test
    public void shouldReturnEnforcementAreaForPostcode() {
        when(referenceDataService.getEnforcementAreaByPostcode(postcode, envelope)).thenReturn(Optional.of(enforcementArea1));

        final JsonObject enforcementArea = enforcementAreaService.getEnforcementArea(postcode, ljaNationalCourtCode, envelope);
        assertThat(enforcementArea, is(enforcementArea1));

        verify(referenceDataService, never()).getEnforcementAreaByLocalJusticeAreaNationalCourtCode(any(), any());
    }

    @Test
    public void shouldReturnEnforcementAreaForLjaIfNoEnforcementAreaForPostcode() {
        when(referenceDataService.getEnforcementAreaByPostcode(postcode, envelope)).thenReturn(Optional.empty());
        when(referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(ljaNationalCourtCode, envelope)).thenReturn(Optional.of(enforcementArea2));

        final JsonObject enforcementArea = enforcementAreaService.getEnforcementArea(postcode, ljaNationalCourtCode, envelope);
        assertThat(enforcementArea, is(enforcementArea2));
    }

    @Test
    public void shouldThrowExceptionWhenEnforcementAreaCanNotBeFound() {
        when(referenceDataService.getEnforcementAreaByPostcode(postcode, envelope)).thenReturn(Optional.empty());
        when(referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(ljaNationalCourtCode, envelope)).thenReturn(Optional.empty());
        try {
            enforcementAreaService.getEnforcementArea(postcode, ljaNationalCourtCode, envelope);
            fail("EnforcementAreaNotFoundException exception expected");
        } catch (final EnforcementAreaNotFoundException e) {
            assertThat(e.getMessage(), is(String.format("Enforcement area not found for postcode = %s nor local justice area national court code = %s", postcode, ljaNationalCourtCode)));
        }
    }

}
