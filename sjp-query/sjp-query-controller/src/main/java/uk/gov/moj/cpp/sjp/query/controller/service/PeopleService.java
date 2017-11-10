package uk.gov.moj.cpp.sjp.query.controller.service;


import static uk.gov.justice.services.core.annotation.Component.QUERY_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
public class PeopleService {

    private static final String QUERY_PERSON = "people.query.person";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_CONTROLLER)
    private Requester requester;

    public JsonEnvelope findPersonByPostcode(JsonEnvelope caseByUrnAndPostcodeQuery, List<String> personIds) {
        String postcode = caseByUrnAndPostcodeQuery.payloadAsJsonObject().getString("postcode");
        for (String personId : personIds) {
            JsonEnvelope person = getPerson(caseByUrnAndPostcodeQuery, personId);
            if (checkPostcodeBelongsTo(postcode, person)) {
                return person;
            }
        }
        return enveloper.withMetadataFrom(caseByUrnAndPostcodeQuery,
                caseByUrnAndPostcodeQuery.metadata().name()).apply(null);
    }

    private JsonEnvelope getPerson(JsonEnvelope jsonEnvelope, String personId) {
        JsonObject payload = Json.createObjectBuilder().add("personId", personId).build();
        JsonEnvelope request = enveloper.withMetadataFrom(jsonEnvelope, QUERY_PERSON).apply(payload);
        return requester.requestAsAdmin(request);
    }


    private boolean checkPostcodeBelongsTo(String postcode, JsonEnvelope person) {
        String inputPostCode = postcode.replaceAll(" ", "");
        if (person.payloadAsJsonObject() != null && !person.payloadAsJsonObject().isEmpty()) {
            String personObjPostCode = person.payloadAsJsonObject().getJsonObject("address")
                    .getString("postCode").replaceAll(" ", "");
            if (inputPostCode.equalsIgnoreCase(personObjPostCode)) {
                return true;
            }
        }
        return false;
    }
}
