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

public class ReferenceDataService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    public JsonObject getEnforcementArea(String postCode){
        final JsonObject queryParams = createObjectBuilder().add("postcode", postCode).build();
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("referencedata.query.enforcement-area"), queryParams);

        return requester.requestAsAdmin(query).payloadAsJsonObject();
    }

}
