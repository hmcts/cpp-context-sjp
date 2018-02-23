package uk.gov.moj.cpp.sjp.query.controller.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonEnvelope response;

    @Test
    public void resolveOffenceTitle() {
        //given
        when(response.payloadAsJsonObject())
                .thenReturn(Json.createReader(getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/referencedata.query.offences.json")).readObject());
        when(requester.request(any(JsonEnvelope.class))).thenReturn(response);

        final String offenceCode = "PS90010";
        final String date = "2017-11-07";

        //when
        final JsonEnvelope envelope = envelope().with(JsonObjectMetadata.metadataWithRandomUUIDAndName()).build();
        final JsonObject offenceReferenceData = referenceDataService.getOffenceReferenceData(envelope, offenceCode, date);

        //then
        assertThat(offenceReferenceData.getString("title"), is("Public service vehicle - passenger use ticket issued for another person"));
        assertThat(offenceReferenceData.getString("legislation"), is("Contrary to regulation 7(1)(b) of the Public Service Vehicles (Conduct of Drivers, Inspectors, Conductors and Passengers) Regulations 1990 and section 25 of the Public Passenger Vehicles Act 1981."));
    }
}
