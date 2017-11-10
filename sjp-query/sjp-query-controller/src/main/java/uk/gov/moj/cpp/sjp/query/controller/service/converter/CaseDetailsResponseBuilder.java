package uk.gov.moj.cpp.sjp.query.controller.service.converter;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class CaseDetailsResponseBuilder {

    private CaseDetailsResponseBuilder() {
        throw new IllegalAccessError("Utility class");
    }

    public static JsonObject buildCaseDetailsResponse(String caseId, JsonObject person, JsonObject defendant) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        add(builder, defendant, "id");
        add(builder, person, "id", "personId");
        add(builder, person, "firstName");
        add(builder, person, "lastName");
        add(builder, person, "dateOfBirth");
        add(builder, person, "nationality");
        add(builder, person, "disability");
        add(builder, person, "ethnicity");
        add(builder, person, "gender");
        builder.add("address", buildAddressJsonObject(person.getJsonObject("address")));
        builder.add("offences", buildOffencesJsonObject(defendant.getJsonArray("offences")));

        return Json.createObjectBuilder().add("caseId", caseId).add("defendant", builder.build()).build();
    }

    private static JsonObject buildAddressJsonObject(JsonObject address) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        add(builder, address, "address1", "addressLine1");
        add(builder, address, "address2", "addressLine2");
        add(builder, address, "address3", "addressLine3");
        add(builder, address, "address4", "addressLine4");
        add(builder, address, "postCode");
        return builder.build();
    }

    private static JsonArray buildOffencesJsonObject(JsonArray offences) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        offences.forEach(offence -> builder.add(buildOffenceJsonObject((JsonObject) offence)));
        return builder.build();
    }

    private static JsonObject buildOffenceJsonObject(JsonObject offence) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("id", offence.getString("id"));
        builder.add("wording", offence.getString("wording"));
        return builder.build();
    }

    public static JsonObjectBuilder add(JsonObjectBuilder builder, JsonObject jsonObject, String fieldName) {
        return add(builder, jsonObject, fieldName, fieldName);
    }

    private static JsonObjectBuilder add(JsonObjectBuilder builder, JsonObject jsonObject, String fieldName1, String fieldName2) {
        return builder.add(fieldName2, jsonObject.containsKey(fieldName1) && jsonObject.get(fieldName1) != JsonValue.NULL ? jsonObject.getString(fieldName1) : "");
    }
}