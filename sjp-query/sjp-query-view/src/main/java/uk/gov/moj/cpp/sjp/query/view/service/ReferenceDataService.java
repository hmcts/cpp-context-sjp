package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;


@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ReferenceDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    public static final String REFERENCEDATA_GET_COURTCENTER = "referencedata.query.organisationunits";
    private static final String RESULTS = "results";
    private static final String REFERENCEDATA_QUERY_PROSECUTORS = "referencedata.query.prosecutors";
    private static final String PROSECUTORS_KEY = "prosecutors";
    public static final String REFERENCEDATA_GET_REFERRAL_REASON_BY_ID = "reference-data.query.get-referral-reason";
    public static final String REFERENCEDATA_GET_OUCODE = "referencedata.query.local-justice-area-court-prosecutor-mapping-courts";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    private static final String QUERY_DATE = LocalDate.now().toString();
    private static final String FIELD_ON = "on";

    public static final String REFERENCEDATA_GET_REFERRAL_REASONS = "referencedata.query.referral-reasons";
    public static final String ID = "id";

    public Optional<JsonArray> getProsecutorsByProsecutorCode(String prosecutorCode) {
        final JsonEnvelope prosecutorsQueryEnvelope = envelopeFrom(
                metadataBuilder().
                        withName(REFERENCEDATA_QUERY_PROSECUTORS).
                        withId(randomUUID()),

                createObjectBuilder().
                        add("prosecutorCode", prosecutorCode));

        final JsonEnvelope prosecutorsData = requester.requestAsAdmin(prosecutorsQueryEnvelope);

        return ofNullable(prosecutorsData.payloadAsJsonObject()).
                map(payload -> payload.getJsonArray(PROSECUTORS_KEY));
    }

    public JsonObject getProsecutor(final String prosecutingAuthority) {
        final JsonObject payload = createObjectBuilder().add("prosecutorCode", prosecutingAuthority).build();
        final JsonEnvelope request = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(REFERENCEDATA_QUERY_PROSECUTORS), payload
        );
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject().getJsonArray(PROSECUTORS_KEY)
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Optional<JsonArray> getAllProsecutors() {
        final JsonEnvelope prosecutorsQueryEnvelope = envelopeFrom(
                metadataBuilder().
                        withName(REFERENCEDATA_QUERY_PROSECUTORS).
                        withId(randomUUID()), createObjectBuilder());

        final JsonEnvelope prosecutorsData = requester.requestAsAdmin(prosecutorsQueryEnvelope);

        return ofNullable(prosecutorsData.payloadAsJsonObject()).
                map(payload -> payload.getJsonArray(PROSECUTORS_KEY));
    }

    public Optional<JsonObject> getOffenceData(String offenceCode) {
        final JsonEnvelope offenceQueryEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName("referencedataoffences.query.offences-list"),
                createObjectBuilder().
                        add("cjsoffencecode", offenceCode));

        final JsonEnvelope offenceRefDataEnvelope = requester.requestAsAdmin(offenceQueryEnvelope);
        final JsonArray offencesArray = offenceRefDataEnvelope.payloadAsJsonObject().getJsonArray("offences");
        if (!offencesArray.isEmpty()) {
            return of(offencesArray.getJsonObject(0));
        } else {
            return empty();
        }
    }

    public JsonArray getReferralReasons() {
        final JsonEnvelope referralReasonsEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName("referencedata.query.referral-reasons"),
                createObjectBuilder());

        return requester.requestAsAdmin(referralReasonsEnvelope)
                .payloadAsJsonObject()
                .getJsonArray("referralReasons");
    }

    public Optional<JsonObject> getReferralReasonByReferralReasonId(final UUID referralReasonId) {

        final JsonEnvelope referralReasonsEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCEDATA_GET_REFERRAL_REASON_BY_ID),
                createObjectBuilder().add(ID, referralReasonId.toString()).build());


        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get referral reasons '{}' received with payload {} ", REFERENCEDATA_GET_REFERRAL_REASONS, referralReasonsEnvelope.payload());
        }

        return Optional.ofNullable(requester.requestAsAdmin(referralReasonsEnvelope)
                .payloadAsJsonObject());
    }


    public List<JsonObject> getWithdrawalReasons(final JsonEnvelope jsonEnvelope) {
        final JsonEnvelope query = enveloper
                .withMetadataFrom(jsonEnvelope, "referencedata.query.offence-withdraw-request-reasons")
                .apply(createObjectBuilder().build());

        return requester.requestAsAdmin(query)
                .payloadAsJsonObject()
                .getJsonArray("offenceWithdrawRequestReasons")
                .getValuesAs(JsonObject.class);
    }

    public List<JsonObject> getResultIds(final JsonEnvelope envelope) {
        final JsonEnvelope query = enveloper
                .withMetadataFrom(envelope, "referencedata.query.results")
                .apply(createObjectBuilder().build());

        final JsonObject resultsResponse = requester.requestAsAdmin(query)
                .payloadAsJsonObject();

        if (resultsResponse.containsKey(RESULTS) && !resultsResponse.isNull(RESULTS)) {
            return resultsResponse.getJsonArray(RESULTS).getValuesAs(JsonObject.class);
        } else {
            return emptyList();
        }
    }

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

    public Optional<JsonObject> getAllFixedList(final JsonEnvelope sourceEvent) {

        final JsonObject queryParams = createObjectBuilder().add(FIELD_ON, QUERY_DATE).build();

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(sourceEvent.metadata())
                .withName("referencedata.get-all-fixed-list"), queryParams);

        final JsonValue fixedList = requester.request(requestEnvelope).payload();

        return JsonValue.NULL.equals(fixedList) ? Optional.empty() : of((JsonObject) fixedList);
    }

    public List<RegionalOrganisation> getRegionalOrganisations(final JsonEnvelope source) {
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataFrom(source.metadata()).withName("referencedata.query.regional-organisations-except-region-name-police"),
                createObjectBuilder()
        );

        final JsonEnvelope response = requester.request(requestEnvelope);

        return JsonValue.NULL.equals(response.payload()) ?
                emptyList() :
                mapRegionalOrganisations(response);
    }

    private List<RegionalOrganisation> mapRegionalOrganisations(final JsonEnvelope response) {
        if (!response.payloadAsJsonObject().containsKey("regionalOrganisations")) {
            return emptyList();
        }

        return response.payloadAsJsonObject().getJsonArray("regionalOrganisations")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(jsonObject -> new RegionalOrganisation(
                        UUID.fromString(jsonObject.getString("id")),
                        jsonObject.getString("regionName"),
                        jsonObject.containsKey("seqNum") ? jsonObject.getInt("seqNum") : null,
                        jsonObject.containsKey("cbwaEnforcerEmail") ? jsonObject.getString("cbwaEnforcerEmail") : null
                ))
                .collect(Collectors.toList());
    }

    public Optional<JsonObject> getNationality(final String nationalityCode) {
        final JsonEnvelope request = envelopeFrom(
                metadataBuilder()
                    .withId(randomUUID())
                    .withName("referencedata.query.country-nationality"),
                createObjectBuilder().build());
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payloadAsJsonObject().getJsonArray("countryNationality")
                .getValuesAs(JsonObject.class).stream()
                .filter(nationality -> nonNull(nationality.getString("isoCode", null)))
                .filter(nationality -> nationality.getString("isoCode").equals(nationalityCode))
                .findFirst();
    }

    public Optional<JsonObject> getEthnicity(final String ethnicityCode) {
        final JsonObject payload = createObjectBuilder().add("code", ethnicityCode).build();
        final JsonEnvelope request = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("referencedata.query.ethnicities"),
                payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payloadAsJsonObject()
                .getJsonArray("ethnicities")
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst();
    }

    public Optional<JsonObject> getCourtCentre(final String defendantPostCode, final String prosecutionAuthorityCode, final JsonEnvelope jsonEnvelope) {

        final Optional<JsonObject> responseOucode = getCourtsByPostCodeAndProsecutingAuthority(jsonEnvelope, defendantPostCode, prosecutionAuthorityCode, requester);
        String oucode = null;
        if (responseOucode.isPresent() && !responseOucode.get().getJsonArray("courts").isEmpty()) {
            final String courtHouseCode = ((JsonObject)
                    responseOucode.get().getJsonArray("courts").get(0)).getString("oucode");
            oucode = courtHouseCode;
        }
        return oucode != null ? getCourtCentreFromReferenceData(oucode, jsonEnvelope, requester) : empty();
    }

    public Optional<JsonObject> getCourtCentreFromReferenceData(final String oucode, final JsonEnvelope jsonEnvelope, final Requester requester) {
        final JsonObject jsonObject = getCourtsOrganisationUnitsByOuCode(jsonEnvelope, oucode, requester).orElseThrow(RuntimeException::new);
        final JsonObject orgUnit = (JsonObject) jsonObject.getJsonArray("organisationunits").get(0);

        return JsonValue.NULL.equals(orgUnit) ? Optional.empty() : of(Json.createObjectBuilder().add("CourtCentre",
                orgUnit).build());

    }

    public Optional<JsonObject> getCourtsOrganisationUnitsByOuCode(final JsonEnvelope event, final String oucode, final Requester requester) {

        final JsonObject payload = Json.createObjectBuilder()
                .add("oucode", oucode)
                .build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_GET_COURTCENTER)
                .withMetadataFrom(event));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get court center '{}' received with payload {} ", REFERENCEDATA_GET_COURTCENTER, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtsByPostCodeAndProsecutingAuthority(final JsonEnvelope jsonEnvelope, final String postcode, final String prosecutingAuthority, final Requester requester) {
        final JsonObject payloadForoucode = Json.createObjectBuilder()
                .add("postcode", postcode)
                .add("prosecutingAuthority", prosecutingAuthority)
                .build();
        final JsonEnvelope responseForoucode = requester.request(envelop(payloadForoucode)
                .withName(REFERENCEDATA_GET_OUCODE)
                .withMetadataFrom(jsonEnvelope));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get oucode '{}' received with payload {} ", REFERENCEDATA_GET_OUCODE, responseForoucode.toObfuscatedDebugString());
        }
        return Optional.ofNullable(responseForoucode.payloadAsJsonObject());
    }
}
