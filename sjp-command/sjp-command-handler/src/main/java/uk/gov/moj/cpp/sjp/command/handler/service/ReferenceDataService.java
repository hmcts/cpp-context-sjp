package uk.gov.moj.cpp.sjp.command.handler.service;

import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter converter;


    public Optional<JsonObject> getEnforcementAreaByPostcode(final String postcode, final JsonEnvelope sourceEvent) {
        final JsonObject queryParams = createObjectBuilder().add("postcode", postcode).build();
        return getEnforcementArea(sourceEvent, queryParams);
    }

    public Optional<JsonObject> getEnforcementAreaByLocalJusticeAreaNationalCourtCode(final String localJusticeAreaNationalCourtCode, final JsonEnvelope sourceEvent) {
        final JsonObject queryParams = createObjectBuilder().add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode).build();
        return getEnforcementArea(sourceEvent, queryParams);
    }

    private Optional<JsonObject> getEnforcementArea(final JsonEnvelope sourceEvent, final JsonObject queryParams) {
        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(sourceEvent, "referencedata.query.enforcement-area.v2")
                .apply(queryParams);

        final JsonValue enforcementArea = requester.request(requestEnvelope).payload();
        return JsonValue.NULL.equals(enforcementArea) ? Optional.empty() : of((JsonObject) enforcementArea);
    }
}
