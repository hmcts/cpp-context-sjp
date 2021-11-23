package uk.gov.moj.cpp.sjp.event.processor.service;


import static java.lang.Integer.valueOf;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.RESULTS;

import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementAreaNotFoundException;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.DocumentTypeAccess;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ReferenceDataService {

    private static final String REFERENCEDATA_GET_DOCUMENT_ACCESS = "referencedata.query.document-type-access";

    private static final String ON_QUERY_PARAMETER = "on";
    public static final String VERDICT_TYPES = "verdictTypes";
    private static final String OU_CODE = "oucode";
    private static final String SHORT_CODE_PARAMETER = "shortCode";
    private static final String RESULT_DEFINITIONS = "resultDefinitions";

    public static final String REFERENCEDATA_GET_REFERRAL_REASON_BY_ID = "reference-data.query.get-referral-reason";
    public static final String ID = "id";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);


    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public Envelope<JsonObject> getOffenceByCjsCode(final String cjsOffenceCode, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("cjsoffencecode", cjsOffenceCode).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(event.metadata()).withName("referencedata.query.offences"), payload);
        return requester.requestAsAdmin(request, JsonObject.class);
    }

    public Optional<JsonObject> getVerdictTypes(final JsonEnvelope event,
                                                final String verdictType) {

        final JsonObject payload = Json.createObjectBuilder().build();

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(payload)
                .withName("referencedata.query.verdict-types")
                .withMetadataFrom(event);


        final JsonObject response = requester.requestAsAdmin(jsonObjectEnvelope, JsonObject.class).payload();
        return response.getJsonArray(VERDICT_TYPES).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> verdictType.equals(jsonObject.getString("verdictType", null)))
                .findFirst();
    }

    public List<JsonObject> getAllVerdictTypes(final JsonEnvelope envelope) {

        final JsonObject payload = Json.createObjectBuilder().build();

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(payload)
                .withName("referencedata.query.verdict-types")
                .withMetadataFrom(envelope);

        final JsonObject response = requester.requestAsAdmin(jsonObjectEnvelope, JsonObject.class).payload();
        return response.getJsonArray(VERDICT_TYPES).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .collect(toList());
    }

    public Envelope<JsonObject> getOffenceByCjsCodeAndHearing(final String cjsOffenceCode, final String offenceDate, JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder()
                .add("q", cjsOffenceCode)
                .add("offenceDate", offenceDate).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(event.metadata()).withName("referencedataoffences.query.offences-list"), payload);

        return requester.requestAsAdmin(request, JsonObject.class);
    }

    public JsonObject getAllNationality(final JsonEnvelope envelope) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata())
                .withName("referencedata.query.country-nationality"), createObjectBuilder().build());
        return requester.requestAsAdmin(request, JsonObject.class).payload();
    }

    public Optional<JsonObject> getNationalityById(final UUID nationalityId, final JsonEnvelope envelope) {
        final JsonObject response = getAllNationality(envelope);
        return response.getJsonArray("countryNationality")
                .getValuesAs(JsonObject.class).stream()
                .filter(nationality -> nonNull(nationality.getString("id", null)))
                .filter(nationality -> nationality.getString("id").equals(nationalityId.toString()))
                .findFirst();
    }

    public Envelope<JsonObject> fetchResultDefinitionById(final JsonEnvelope envelope, final LocalDate on, final UUID id) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("referencedata.get-result-definition"), createObjectBuilder().add("on", on.toString()).add("resultDefinitionId", id.toString()).build());
        return requester.requestAsAdmin(request, JsonObject.class);
    }

    public JsonObject getOrgainsationUnit(final String courtId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("id", courtId).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName("referencedata.query.organisation-unit.v2"), payload);
        return requester.requestAsAdmin(requestEnvelope, JsonObject.class).payload();
    }

    public JsonObject getCourtRoomOuCode(final String courtRoomUuid) {
        final JsonObject payload = createObjectBuilder().add("courtRoomUuid", courtRoomUuid).build();
        final String REFERENCE_DATA_QUERY_GET_POLICE_COURT_ROOM_CODE = "REFERENCE_DATA_QUERY_GET_POLICE_COURT_ROOM_CODE";
        final JsonEnvelope requestEnvelope = envelopeFrom(Envelope.metadataBuilder().withName(REFERENCE_DATA_QUERY_GET_POLICE_COURT_ROOM_CODE).withId(randomUUID()).build(), payload);
        return requester.requestAsAdmin(requestEnvelope, JsonObject.class).payload();
    }

    public boolean getSpiOutFlagForProsecutorOucode(final String oucode) {
        final JsonObject payload = createObjectBuilder().add(OU_CODE, oucode).build();
        final String REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE = "REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE";
        final JsonEnvelope request = envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName(REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE).build(), payload);
        final JsonObject response = requester.requestAsAdmin(request, JsonObject.class).payload();
        return response.getBoolean("spiOutFlag");
    }

    public List<BailStatus> getAllBailStatuses(final JsonEnvelope context) {
        final JsonObject payload = createObjectBuilder().build();
        final Metadata metadata = envelop(payload).withName("referencedata.query.bail-statuses").withMetadataFrom(context).metadata();
        final JsonEnvelope request = envelopeFrom(metadata, payload);
        final JsonObject response = requester.requestAsAdmin(request, JsonObject.class).payload();

        return Optional.ofNullable(response)
                .map(o -> o.getJsonArray("bailStatuses"))
                .map(a -> a.getValuesAs(JsonObject.class))
                .map(l -> l.stream()
                        .map(this::convertBailStatus))
                .orElseGet(Stream::empty)
                .collect(toList());

    }

    private BailStatus convertBailStatus(final JsonObject source) {
        return bailStatus()
                .withId(fromString(source.getString("id", null)))
                .withCode(source.getString("statusCode", null))
                .withDescription(source.getString("statusDescription", null))
                .build();
    }

    public List<AllocationDecision> getAllModeOfTrialReasons(final JsonEnvelope context) {
        final JsonObject payload = createObjectBuilder().build();
        final Metadata metadata = envelop(payload).withName("referencedata.query.mode-of-trial-reasons").withMetadataFrom(context).metadata();
        final JsonEnvelope request = envelopeFrom(metadata, payload);
        final JsonObject response = requester.requestAsAdmin(request, JsonObject.class).payload();

        return Optional.ofNullable(response)
                .map(o -> o.getJsonArray("modeOfTrialReasons"))
                .map(a -> a.getValuesAs(JsonObject.class))
                .map(l -> l.stream()
                        .map(this::convertToAllocationDecision))
                .orElseGet(Stream::empty)
                .collect(toList());
    }

    private AllocationDecision convertToAllocationDecision(final JsonObject jsonObject) {
        return allocationDecision()
                .withMotReasonId(fromString(jsonObject.getString("id")))
                .withSequenceNumber(valueOf(jsonObject.getInt("seqNum")))
                .withMotReasonCode(jsonObject.getString("code"))
                .withMotReasonDescription(jsonObject.getString("description"))
                .build();
    }

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

    public JsonObject getProsecutors(final String prosecutingAuthority, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("prosecutorCode", prosecutingAuthority).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.prosecutors").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public Optional<JsonObject> getProsecutor(final String prosecutingAuthority, final JsonEnvelope envelope) {
        return getProsecutors(prosecutingAuthority, envelope)
                .getJsonArray("prosecutors")
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst();
    }

    public JsonObject getProsecutorByPtiUrn(final String ptiurn, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("ptiurn", ptiurn).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.prosecutor.by.ptiurn").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }


    public String getProsecutor(final String prosecutingAuthority, final Boolean isWelsh, final JsonEnvelope envelope) {
        final JsonObject prosecutor = this.getProsecutors(prosecutingAuthority, envelope)
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

    public JsonObject getReferralReasons(final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope,
                "referencedata.query.referral-reasons")
                .apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public JsonObject getFixedList(final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope,
                "referencedata.get-all-fixed-list")
                .apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public List<DocumentTypeAccess> getDocumentTypeAccess(final LocalDate date, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("date", date.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(
                envelope, "referencedata.get-all-document-type-access").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payloadAsJsonObject()
                .getJsonArray("documentsTypeAccess")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(e -> new DocumentTypeAccess(fromString(e.getString("id")), e.getString("section")))
                .collect(toList());
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

    public Optional<JsonObject> getCourtByCourtHouseOUCode(final String courtHouseOUCode, final JsonEnvelope envelope) {
        final JsonObject queryParams = createObjectBuilder().add(OU_CODE, courtHouseOUCode).build();
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

    public LocalJusticeArea getLocalJusticeAreaByCode(final JsonEnvelope envelope, final String localJusticeAreaNationalCourtCode) {
        final JsonObject queryParams = createObjectBuilder()
                .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                .build();
        return getEnforcementArea(envelope, queryParams)
                .map(enforcementArea -> LocalJusticeArea.fromJson(enforcementArea.getJsonObject("localJusticeArea")))
                .orElseThrow(() -> new EnforcementAreaNotFoundException("Could not find Local Justice Area by code: " + localJusticeAreaNationalCourtCode));
    }

    public Optional<String> getDvlaPenaltyPointNotificationEmailAddress(final JsonEnvelope envelope) {
        final JsonObject queryParams = createObjectBuilder()
                .add("orgName", "DVLA Penalty Point Notification")
                .build();

        final JsonEnvelope requestEnvelope = enveloper
                .withMetadataFrom(envelope, "referencedata.query.organisation.byorgname")
                .apply(queryParams);

        final JsonValue dvlaEmailAddress = requester.requestAsAdmin(requestEnvelope).payload();
        return optionalResponse(dvlaEmailAddress).map(o -> o.getString("emailAddress"));
    }

    public Optional<JsonObject> getEnforcementAreaByPostcode(final String postcode, final JsonEnvelope sourceEvent) {
        final JsonObject queryParams = createObjectBuilder().add("postcode", postcode).build();
        return getEnforcementArea(sourceEvent, queryParams);
    }

    public Optional<JsonObject> getEnforcementAreaByLocalJusticeAreaNationalCourtCode(final String localJusticeAreaNationalCourtCode, final JsonEnvelope sourceEvent) {
        final JsonObject queryParams = createObjectBuilder().add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode).build();
        return getEnforcementArea(sourceEvent, queryParams);
    }

    private Optional<JsonObject> optionalResponse(final JsonValue response) {
        return JsonValue.NULL.equals(response) ? empty() : of((JsonObject) response);
    }

    private Optional<JsonObject> getEnforcementArea(final JsonEnvelope sourceEvent, final JsonObject queryParams) {
        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(sourceEvent, "referencedata.query.enforcement-area.v2")
                .apply(queryParams);

        final JsonValue enforcementArea = requester.requestAsAdmin(requestEnvelope).payload();
        return JsonValue.NULL.equals(enforcementArea) ? Optional.empty() : of((JsonObject) enforcementArea);
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

    public Optional<JsonObject> getDocumentTypeAccessData(final UUID documentTypeId, final JsonEnvelope jsonEnvelope, final Requester requester) {
        final JsonObject payload = Json.createObjectBuilder().add(ID, documentTypeId.toString()).build();
        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_GET_DOCUMENT_ACCESS)
                .withMetadataFrom(jsonEnvelope));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" '{}' by id {} received with payload {} ", REFERENCEDATA_GET_DOCUMENT_ACCESS, documentTypeId, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getReferralReasonByReferralReasonId(final UUID referralReasonId) {

        final JsonEnvelope referralReasonsEnvelope = envelopeFrom(
                Envelope.metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCEDATA_GET_REFERRAL_REASON_BY_ID),
                createObjectBuilder().add(ID, referralReasonId.toString()).build());


        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get referral reasons '{}' received with payload {} ", REFERENCEDATA_GET_REFERRAL_REASON_BY_ID, referralReasonsEnvelope.payload());
        }

        return Optional.ofNullable(requester.requestAsAdmin(referralReasonsEnvelope)
                .payloadAsJsonObject());
    }


}
