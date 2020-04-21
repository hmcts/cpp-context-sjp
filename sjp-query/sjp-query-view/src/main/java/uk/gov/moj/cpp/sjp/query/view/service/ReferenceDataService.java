package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ReferenceDataService {

    private static final String RESULTS = "results";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    private static final String QUERY_DATE = LocalDate.now().toString();
    private static final String FIELD_ON = "on";

    public Optional<JsonArray> getProsecutorsByProsecutorCode(String prosecutorCode) {
        final JsonEnvelope prosecutorsQueryEnvelope = envelopeFrom(
                metadataBuilder().
                        withName("referencedata.query.prosecutors").
                        withId(randomUUID()),

                createObjectBuilder().
                        add("prosecutorCode", prosecutorCode));

        final JsonEnvelope prosecutorsData = requester.requestAsAdmin(prosecutorsQueryEnvelope);

        return ofNullable(prosecutorsData.payloadAsJsonObject()).
                map(payload -> payload.getJsonArray("prosecutors"));
    }

    public Optional<JsonArray> getAllProsecutors() {
        final JsonEnvelope prosecutorsQueryEnvelope = envelopeFrom(
                metadataBuilder().
                        withName("referencedata.query.prosecutors").
                        withId(randomUUID()), createObjectBuilder());

        final JsonEnvelope prosecutorsData = requester.requestAsAdmin(prosecutorsQueryEnvelope);

        return ofNullable(prosecutorsData.payloadAsJsonObject()).
                map(payload -> payload.getJsonArray("prosecutors"));
    }

    public Optional<JsonObject> getOffenceData(String offenceCode){
        final JsonEnvelope offenceQueryEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName("referencedataoffences.query.offences-list"),
                createObjectBuilder().
                        add("cjsoffencecode", offenceCode));

        final JsonEnvelope offenceRefDataEnvelope = requester.requestAsAdmin(offenceQueryEnvelope);
        final JsonArray offencesArray = offenceRefDataEnvelope.payloadAsJsonObject().getJsonArray("offences");
        if(!offencesArray.isEmpty()){
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

        if(resultsResponse.containsKey(RESULTS) && !resultsResponse.isNull(RESULTS)){
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

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(sourceEvent.metadata()).withName("referencedata.get-all-fixed-list"), queryParams);

        final JsonValue fixedList = requester.request(requestEnvelope).payload();

        return JsonValue.NULL.equals(fixedList) ? Optional.empty() : of((JsonObject) fixedList);
    }
}
