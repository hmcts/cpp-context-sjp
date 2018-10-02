package uk.gov.moj.cpp.sjp.event.processor.service;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public String getCountryByPostcode(final String postCode, final JsonEnvelope envelope){
        final JsonObject payload = Json.createObjectBuilder().add("postCode", postCode).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.country-by-postcode").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject().getString("country");
    }

}
