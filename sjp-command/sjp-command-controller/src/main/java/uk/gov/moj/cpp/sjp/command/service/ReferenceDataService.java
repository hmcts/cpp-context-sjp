package uk.gov.moj.cpp.sjp.command.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ReferenceDataService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    public JsonObject getEnforcementArea(String postCode){
        final JsonObject queryParams = createObjectBuilder().add("postcode", postCode).build();
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("referencedata.query.enforcement-area"), queryParams);

        final JsonEnvelope responseEnvelope = requester.requestAsAdmin(query);
        if(responseEnvelope.payload().getValueType().equals(JsonValue.ValueType.OBJECT)){
            return responseEnvelope.payloadAsJsonObject();
        } else {
            return null;
        }
    }

    public JsonObject getLocalJusticeAreas(final String nationalCourtCode){
        final JsonObject queryParams = createObjectBuilder().add("nationalCourtCode", nationalCourtCode).build();
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("referencedata.query.local-justice-areas"), queryParams);

        final JsonEnvelope responseEnvelope = requester.requestAsAdmin(query);
        if(responseEnvelope.payload().getValueType().equals(JsonValue.ValueType.OBJECT)){
            return responseEnvelope.payloadAsJsonObject();
        } else {
            return null;
        }
    }
}
