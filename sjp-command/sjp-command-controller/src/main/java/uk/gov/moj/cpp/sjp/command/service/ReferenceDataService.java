package uk.gov.moj.cpp.sjp.command.service;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ReferenceDataService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    public Optional<JsonObject> getEnforcementArea(String postCode){
        final JsonObject queryParams = createObjectBuilder().add("postcode", postCode).build();
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("referencedata.query.enforcement-area"), queryParams);

        final JsonEnvelope response = requester.requestAsAdmin(query);
        return getNullablePayload(response);
    }

    public Optional<JsonObject> getLocalJusticeAreas(final String nationalCourtCode){
        final JsonObject queryParams = createObjectBuilder().add("nationalCourtCode", nationalCourtCode).build();
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("referencedata.query.local-justice-areas"), queryParams);

        final JsonEnvelope response = requester.requestAsAdmin(query);
        return getNullablePayload(response);
    }

    public Optional<JsonObject> getReferralReason(final String referralReasonId) {
        final JsonObject queryParams = createObjectBuilder().add("id", referralReasonId).build();
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("reference-data.query.get-referral-reason"), queryParams);

        final JsonEnvelope response = requester.requestAsAdmin(query);
        return getNullablePayload(response);
    }

    private Optional<JsonObject> getNullablePayload(final JsonEnvelope response) {
        return response.payload() != JsonValue.NULL
                ? ofNullable(response.payloadAsJsonObject())
                : empty();
    }
}
