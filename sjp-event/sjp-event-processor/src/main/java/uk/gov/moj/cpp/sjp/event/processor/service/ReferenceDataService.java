package uk.gov.moj.cpp.sjp.event.processor.service;


import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ReferenceDataService {

    private static final String ON_QUERY_PARAMETER = "on";
    private static final String SHORT_CODE_PARAMETER = "shortCode";
    private static final String RESULT_DEFINITIONS = "resultDefinitions";

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
                .filter(nationality -> nonNull(nationality.getString("isoCode", null)))
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

    public JsonObject getProsecutor(final String prosecutingAuthority, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("prosecutorCode", prosecutingAuthority).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.prosecutors").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public String getProsecutor(final String prosecutingAuthority, final Boolean isWelsh, final JsonEnvelope envelope) {
        final JsonObject prosecutor = this.getProsecutor(prosecutingAuthority, envelope)
                .getJsonArray("prosecutors")
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst()
                .orElse(null);

        if (Objects.isNull(prosecutor)) {
            return prosecutingAuthority;
        }

        return isWelsh
                ? prosecutor.getString("nameWelsh", prosecutor.getString("fullName", prosecutingAuthority))
                : prosecutor.getString("fullName", prosecutingAuthority);
    }

    public String getProsecutorOucode(final String prosecutingAuthority, final Envelope envelope) {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), createObjectBuilder().build());
        return getProsecutor(prosecutingAuthority, jsonEnvelope)
                .getJsonArray("prosecutors")
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst()
                .map(p -> p.getString("oucode", null))
                .orElse(null);
    }

    public JsonObject getReferralReasons(final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope,
                "referencedata.query.referral-reasons")
                .apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public JsonObject getDocumentTypeAccess(final LocalDate date, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("date", date.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope, "referencedata.get-all-document-type-access").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public JsonObject getHearingTypes(final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope,
                "referencedata.query.hearing-types")
                .apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public JsonArray getAllResultDefinitions(final JsonEnvelope envelope, final LocalDate onDate) {

        final Envelope<JsonObject> request = Enveloper
                .envelop(
                        createObjectBuilder()
                                .add(ON_QUERY_PARAMETER, onDate.toString())
                                .build()
                )
                .withName("referencedata.get-all-result-definitions")
                .withMetadataFrom(envelope);

        return requester.request(request)
                .payloadAsJsonObject()
                .getJsonArray(RESULT_DEFINITIONS);
    }

    public Optional<JsonObject> getResultDefinition(final String shortCode, final LocalDate onDate) {
        final JsonObject payload = createObjectBuilder()
                .add(ON_QUERY_PARAMETER, onDate.toString())
                .add(SHORT_CODE_PARAMETER, shortCode)
                .build();
        
        final JsonEnvelope request = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("referencedata.query-result-definitions").build(),
                payload);

        return ofNullable(requester.requestAsAdmin(request).payloadAsJsonObject()
                .getJsonArray(RESULT_DEFINITIONS))
                .filter(resultDefinitions -> !resultDefinitions.isEmpty())
                .map(resultDefinitions -> resultDefinitions.getJsonObject(0));
    }

    public Optional<JsonObject> getCourtByCourtHouseOUCode(final String courtHouseOUCode, final JsonEnvelope envelope) {
        final JsonObject queryParams = createObjectBuilder().add("oucode", courtHouseOUCode).build();
        final JsonEnvelope query = enveloper.withMetadataFrom(envelope, "referencedata.query.organisationunits")
                .apply(queryParams);

        final JsonEnvelope organisationUnitsResponse = requester.requestAsAdmin(query);

        return organisationUnitsResponse.payloadAsJsonObject()
                .getJsonArray("organisationunits")
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst();
    }

    public JsonArray getAllResultDefinitions(final JsonEnvelope envelope) {

        final Envelope<JsonObject> request = Enveloper
                .envelop(createObjectBuilder().build())
                .withName("referencedata.get-all-result-definitions")
                .withMetadataFrom(envelope);

        return requester.request(request)
                .payloadAsJsonObject()
                .getJsonArray(RESULT_DEFINITIONS);
    }
}
