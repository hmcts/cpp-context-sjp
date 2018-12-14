package uk.gov.moj.cpp.sjp.event.processor.service;


import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.json.schemas.domains.sjp.ProsecutingAuthority;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public String getCountryByPostcode(final String postCode, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("postCode", postCode).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.country-by-postcode").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject().getString("country");
    }

    public Optional<JsonObject> getNationality(final String nationalityCode, final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.country-nationality").apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payloadAsJsonObject().getJsonArray("countryNationality")
                .getValuesAs(JsonObject.class).stream()
                .filter(nationality -> nationality.getString("isoCode").equals(nationalityCode))
                .findFirst();
    }

    public Optional<JsonObject> getEthnicity(final String ethnicityCode, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("code", ethnicityCode).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.ethnicities").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payloadAsJsonObject()
                .getJsonArray("ethnicities")
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst();
    }

    public JsonObject getProsecutor(final ProsecutingAuthority prosecutingAuthority, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("prosecutorCode", prosecutingAuthority.name()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.prosecutors").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public JsonObject getReferralReasons(final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope,
                "referencedata.query.referral-reasons")
                .apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public JsonObject getDocumentMetadata(final LocalDate date, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("date", date.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope, "referencedata.get-all-document-metadata").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }
}
