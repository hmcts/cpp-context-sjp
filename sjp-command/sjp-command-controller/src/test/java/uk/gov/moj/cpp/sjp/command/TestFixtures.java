package uk.gov.moj.cpp.sjp.command;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class TestFixtures {

    public static JsonObject getDefendantDetailsPayload(final String personId, final String postcode, final String firstName) {
        return createObjectBuilder()
                .add("personId", personId)
                .add("address", getAddressPayload(postcode))
                .add("firstName", firstName)
                .build();
    }

    public static JsonObject getAddressPayload(final String postcode) {
        return createObjectBuilder()
                .add("address1", "14 Tottenham Court Road")
                .add("postcode", postcode)
                .build();
    }

    public static JsonObject getEnforcementAreaPayload(final int enforcingCourtCode, final String nationalCourtCode) {
        return createObjectBuilder()
                .add("enforcingCourtCode", enforcingCourtCode)
                .add("localJusticeArea", createObjectBuilder().add("nationalCourtCode", nationalCourtCode).build())
                .build();
    }

    public static JsonObject getLocalJusticeAreasPayload(final String region, final String nationalCourtCode) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        ofNullable(nationalCourtCode).ifPresent(obj -> jsonObjectBuilder.add("nationalCourtCode", nationalCourtCode));
        ofNullable(region).ifPresent(e -> jsonObjectBuilder.add("region", e));

        final JsonArray localJusticeAreaArray = createArrayBuilder()
                .add(jsonObjectBuilder)
                .build();
        return createObjectBuilder()
                .add("localJusticeAreas", localJusticeAreaArray)
                .build();
    }
}
