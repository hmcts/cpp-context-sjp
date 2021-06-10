package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS_LINE_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.COURT_CENTRE_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.COURT_CENTRE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.COURT_CENTRE_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.COURT_CENTRE_WELSH_ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.COURT_CENTRE_WELSH_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LJA_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS_LINE_1;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtCentreConverterTest {

    @InjectMocks
    CourtCentreConverter courtCentreConverter;

    @Mock
    JsonObject sjpSessionPayload;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private SjpService sjpService;

    @Mock
    private LJADetailsConverter ljaDetailsConverter;

    @Mock
    private AddressConverter addressConverter;

    @Mock
    Metadata sourceMetadata;

    @Mock
    LjaDetails ljaDetails;

    @Test
    public void shouldConvertCourtCentre() {

        final Address address = Address.address().withAddress1(ADDRESS_LINE_1).build();
        final Address welshAddress = Address.address().withAddress1(WELSH_ADDRESS_LINE_1).build();
        final UUID sjpSessionId = UUID.randomUUID();
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("name", "test")
                .build();

        final JsonObject sjpSessionPayload = Json.createObjectBuilder()
                .add("courtHouseCode", UUID.randomUUID().toString())
                .build();

        final Optional<JsonObject> courtOptional = Optional.of(Json.createObjectBuilder()
                .add("id", COURT_CENTRE_ID.toString())
                .add("isWelsh", true)
                .add("oucode", COURT_CENTRE_CODE)
                .add("idWelsh", COURT_CENTRE_WELSH_ID.toString())
                .add("oucodeL3Name", COURT_CENTRE_NAME)
                .add("oucodeL3WelshName", COURT_CENTRE_WELSH_NAME)
                .build());

        when(sourceMetadata.asJsonObject()).thenReturn(jsonObject);
        when(sjpService.getSessionDetails(anyObject(), anyObject())).thenReturn(sjpSessionPayload);
        when(referenceDataService.getCourtByCourtHouseOUCode(anyObject(), anyObject())).thenReturn(courtOptional);
        when(addressConverter.convertWelsh(anyObject())).thenReturn(welshAddress);
        when(addressConverter.convert(anyObject())).thenReturn(address);
        when(ljaDetailsConverter.convert(anyObject(), anyObject())).thenReturn(ljaDetails);
        when(ljaDetails.getLjaCode()).thenReturn(LJA_CODE);


        final CourtCentre courtCentre = courtCentreConverter.convert(sjpSessionId, sourceMetadata);

        assertThat(courtCentre.getId(), is(COURT_CENTRE_ID));
        assertThat(courtCentre.getName(), is(COURT_CENTRE_NAME));
        assertThat(courtCentre.getCode(), is(COURT_CENTRE_CODE));
        assertThat(courtCentre.getWelshCourtCentre(), is(true));
        assertThat(courtCentre.getWelshName(), is(COURT_CENTRE_WELSH_NAME));
        assertThat(courtCentre.getAddress().getAddress1(), is(ADDRESS_LINE_1));
        assertThat(courtCentre.getWelshAddress().getAddress1(), is(WELSH_ADDRESS_LINE_1));
        assertThat(courtCentre.getLja().getLjaCode(), is(LJA_CODE));


    }


}
