package uk.gov.moj.cpp.sjp.query.controller.service;


import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@SuppressWarnings("WeakerAccess")
public class PeopleService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_CONTROLLER)
    private Requester requester;

    @Inject
    private ReferenceDataService referenceDataService;

    /**
     * Add the person info for the defendant matching the given postcode
     * Also only returns a subset of the case attributes
     *
     * @param query for the metadata
     * @return caseDetails with person info added to the relevant defendant, or JsonValue.NULL if
     * there is no match
     */
    public JsonValue addPersonInfoForDefendantWithMatchingPostcode(final String postcode, final JsonObject caseDetails, final JsonEnvelope query) {
        final JsonArray defendants = buildDefendantsArray(query, caseDetails, postcode);
        if (defendants.isEmpty()) {
            return null;
        }
        return buildCaseObject(caseDetails, defendants);
    }

    private JsonArray buildDefendantsArray(JsonEnvelope query, JsonObject caseDetails, String postcode) {
        final JsonArrayBuilder defendantsArrayBuilder = createArrayBuilder();
        for (final JsonObject defendant : caseDetails.getJsonArray("defendants").getValuesAs(JsonObject.class)) {
            buildDefendantObject(query, defendant, postcode).ifPresent(defendantsArrayBuilder::add);
        }
        return defendantsArrayBuilder.build();
    }

    private Optional<JsonObject> buildDefendantObject(final JsonEnvelope query, final JsonObject defendant, final String postcode) {
        // Assume person will never be JsonValue.NULL
        final JsonObject person = (JsonObject) getPerson(defendant.getString("personId"), query);
        if (!hasMatchingPostcode(person, postcode)) {
            return Optional.empty();
        }
        final JsonObjectBuilder defendantBuilder = createObjectBuilder();
        defendantBuilder.add("id", defendant.getString("id"));
        defendantBuilder.add("person", buildPersonObject(person));
        defendantBuilder.add("offences", buildOffencesArray(query, defendant.getJsonArray("offences")));
        return Optional.of(defendantBuilder.build());
    }

    public JsonValue getPerson(final String personId, final JsonEnvelope query) {
        return requester.request(enveloper.withMetadataFrom(query, "people.query.person")
                .apply(createObjectBuilder().add("personId", personId).build())).payload();
    }

    private boolean hasMatchingPostcode(JsonObject person, String postcode) {
        return person.getJsonObject("address").getString("postCode").replaceAll(" ", "")
                .equalsIgnoreCase(postcode.replaceAll(" ", ""));
    }

    private JsonObject buildPersonObject(JsonObject person) {
        final JsonObjectBuilder personBuilder = createObjectBuilder();
        personBuilder.add("id", person.getString("id"));
        personBuilder.add("firstName", person.getString("firstName"));
        personBuilder.add("lastName", person.getString("lastName"));
        personBuilder.add("dateOfBirth", person.getString("dateOfBirth"));
        add(personBuilder, person, "homeTelephone");
        add(personBuilder, person, "mobile");
        add(personBuilder, person, "email");
        add(personBuilder, person, "nationalInsuranceNumber");
        personBuilder.add("address", person.getJsonObject("address"));
        return personBuilder.build();
    }

    private JsonArray buildOffencesArray(final JsonEnvelope query, final JsonArray offences) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        offences.getValuesAs(JsonObject.class)
                .stream()
                .map(offence -> buildOffenceObject(query, offence)).forEach(builder::add);
        return builder.build();
    }

    private JsonObject buildOffenceObject(final JsonEnvelope query, final JsonObject offence) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("id", offence.getString("id"))
                .add("wording", offence.getString("wording"))
                .add("pendingWithdrawal", offence.getBoolean("pendingWithdrawal", false));
        final String offenceTitle = referenceDataService
                .resolveOffenceTitle(query, offence.getString("offenceCode"), offence.getString("startDate"));
        if (offenceTitle != null) {
            builder.add("title", offenceTitle);
        }
        if (offence.containsKey("plea")) {
            builder.add("plea", offence.getString("plea"));
        }
        return builder.build();
    }

    private JsonObject buildCaseObject(JsonObject caseDetails, JsonArray defendants) {
        return createObjectBuilder()
                .add("id", caseDetails.getString("id"))
                .add("completed", caseDetails.getBoolean("completed", false))
                .add("assigned", caseDetails.getBoolean("assigned", false))
                .add("defendants", defendants)
                .build();
    }

    private void add(final JsonObjectBuilder builder, final JsonObject jsonObject, final String propertyName) {
        if (jsonObject.containsKey(propertyName)) {
            builder.add(propertyName, jsonObject.getString(propertyName));
        } else {
            builder.addNull(propertyName);
        }
    }
}
