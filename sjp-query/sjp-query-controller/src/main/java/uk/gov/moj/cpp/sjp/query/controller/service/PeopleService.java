package uk.gov.moj.cpp.sjp.query.controller.service;


import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

@SuppressWarnings("WeakerAccess")
public class PeopleService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_CONTROLLER)
    private Requester requester;

    public JsonValue getPerson(final String personId, final JsonEnvelope query) {
        return requester.requestAsAdmin(enveloper.withMetadataFrom(query, "people.query.person")
                .apply(createObjectBuilder().add("personId", personId).build())).payload();
    }

    /**
     * Add the person info for the defendant matching the given postcode
     * Also only returns a subset of the case attributes
     *
     * @param query for the metadata
     * @return caseDetails with person info added to the relevant defendant, or JsonValue.NULL if
     * there is no match
     */
    public JsonValue addPersonInfoForDefendantWithMatchingPostcode(final String postcode, final JsonObject caseDetails, final JsonEnvelope query) {
        final JsonArrayBuilder defendantsArrayBuilder = createArrayBuilder();
        for (final JsonObject defendant : caseDetails.getJsonArray("defendants").getValuesAs(JsonObject.class)) {
            // Assume person will never be JsonValue.NULL
            final JsonObject person = (JsonObject) getPerson(defendant.getString("personId"), query);
            if (person.getJsonObject("address").getString("postcode").replaceAll(" ", "")
                    .equalsIgnoreCase(postcode.replaceAll(" ", ""))) {
                defendantsArrayBuilder.add(createObjectBuilder()
                        .add("id", defendant.getString("id"))
                        .add("person", person)
                        .add("offences", defendant.getJsonArray("offences")));
            }
        }
        final JsonArray defendants = defendantsArrayBuilder.build();
        if (!defendants.isEmpty()) {
            return createObjectBuilder()
                    .add("id", caseDetails.getString("id"))
                    .add("completed", caseDetails.getBoolean("completed", false))
                    .add("defendants", defendants).build();
        } else {
            return null;
        }
    }
}
