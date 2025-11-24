package uk.gov.moj.cpp.sjp.query.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.sjp.query.api.service.ReferenceOffencesDataService;

@ExtendWith(MockitoExtension.class)
public class ReferenceOffencesDataServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceOffencesDataService referenceOffencesDataService;

    @Mock
    private JsonEnvelope response;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestCaptor;

    @Test
    public void shouldGetOffenceReferenceData() {

        //given
        final JsonEnvelope queryJsonEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode")).build();

        final String offenceCode = "CA03010";
        final String date = "2017-11-07";

        when(requester.request(any(JsonEnvelope.class))).thenReturn(response);

        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("offences", Json.createArrayBuilder().add(Json.createObjectBuilder()))
                .build();

        when(response.payloadAsJsonObject()).thenReturn(responsePayload);

        //when
        final JsonObject result = referenceOffencesDataService.getOffenceReferenceData(queryJsonEnvelope, offenceCode, date);

        //then
        verify(requester).request(requestCaptor.capture());
        final JsonObject requestPayload = requestCaptor.getValue().payloadAsJsonObject();
        assertEquals(requestPayload.getString("cjsoffencecode"), offenceCode);
        assertEquals(requestPayload.getString("date"), date);

        assertEquals(result, responsePayload.getJsonArray("offences").getJsonObject(0));
    }

    @Test
    public void shouldGetOffenceReferenceDataByOffenceId() {
        final JsonEnvelope queryJsonEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode")).build();

        final UUID offenceId = UUID.randomUUID();
        final JsonObject responsePayload = Json.createObjectBuilder().add("offenceId", offenceId.toString()).build();

        when(requester.request(any(JsonEnvelope.class))).thenReturn(response);
        when(response.payloadAsJsonObject()).thenReturn(responsePayload);

        final JsonObject result = referenceOffencesDataService.getOffenceReferenceDataByOffenceId(queryJsonEnvelope, offenceId.toString());

        verify(requester).request(requestCaptor.capture());
        final JsonObject requestPayload = requestCaptor.getValue().payloadAsJsonObject();
        assertEquals(requestPayload.getString("offenceId"), offenceId.toString());
        assertEquals(result, responsePayload);


    }
}
