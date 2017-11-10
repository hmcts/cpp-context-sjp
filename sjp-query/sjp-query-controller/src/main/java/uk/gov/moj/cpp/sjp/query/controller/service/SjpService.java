package uk.gov.moj.cpp.sjp.query.controller.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_CONTROLLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.controller.service.converter.CaseDetailsResponseBuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(QUERY_CONTROLLER)
public class SjpService {

    @Inject
    private Enveloper enveloper;

    @Inject
    private PeopleService peopleService;

    //FIXME: remove once framework is fixed
    @Handles("nothing.nothing.assignment")
    public void doNothing(JsonEnvelope envelope) {
        throw new UnsupportedOperationException();
    }

    public JsonEnvelope getQueryEnvelope(String queryName, final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, queryName).apply(envelope.payloadAsJsonObject());
    }

    public JsonEnvelope findPersonByPostcode(JsonEnvelope sjpCaseDetails, JsonEnvelope caseByUrnAndPostcodeQuery) {
        JsonArray defendants = sjpCaseDetails.payloadAsJsonObject().getJsonArray("defendants");
        List<String> personIds = defendants.stream()
                .map(e -> ((JsonObject) e).getString("personId"))
                .collect(Collectors.toList());
        return peopleService.findPersonByPostcode(caseByUrnAndPostcodeQuery, personIds);
    }

    public JsonEnvelope buildCaseDetailsResponse(String responseName, JsonEnvelope caseDetails, JsonEnvelope personDetails) {
        JsonObject person = personDetails.payloadAsJsonObject();
        JsonObject caseObject = caseDetails.payloadAsJsonObject();
        JsonArray defendants = caseObject.getJsonArray("defendants");

        Optional<JsonValue> found = defendants
                .stream()
                .filter(e -> ((JsonObject) e).getString("personId").equals(personDetails.payloadAsJsonObject().getString("id")))
                .findFirst();

        JsonObject response = null;
        if (found.isPresent()) {
            JsonObject defendant = (JsonObject) found.get();
            String caseId = caseObject.getString("id");
            response = CaseDetailsResponseBuilder.buildCaseDetailsResponse(caseId, person, defendant);
        }
        return enveloper.withMetadataFrom(caseDetails, responseName).apply(response);
    }
}
