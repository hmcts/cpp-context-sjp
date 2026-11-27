package uk.gov.moj.cpp.sjp.command.handler.service;

import static java.util.Optional.of;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceDataService {

    private static final String REFERENCEDATA_QUERY_PROSECUTOR = "referencedata.query.prosecutor";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String ID = "id";

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

    public Optional<JsonObject> getProsecutor(final JsonEnvelope event, final UUID id) {

        LOGGER.info(" Calling {} to get prosecutors for {} ", REFERENCEDATA_QUERY_PROSECUTOR, id);

        final JsonObject payload = createObjectBuilder().add(ID, id.toString()).build();

        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_PROSECUTOR)
                .apply(payload);

        final JsonValue prosecutor = requester.request(requestEnvelope).payload();
        return JsonValue.NULL.equals(prosecutor) ? Optional.empty() : of((JsonObject) prosecutor);
    }
}
