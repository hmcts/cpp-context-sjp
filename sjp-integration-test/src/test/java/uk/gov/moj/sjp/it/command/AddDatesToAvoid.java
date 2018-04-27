package uk.gov.moj.sjp.it.command;

import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class AddDatesToAvoid {

    public static void addDatesToAvoid(final UUID caseId, final String datesToAvoid) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("datesToAvoid", datesToAvoid)
                .build();
        final String url = String.format("/cases/%s/dates-to-avoid", caseId);
        makePostCall(url, "application/vnd.sjp.add-dates-to-avoid+json", payload.toString());
    }
}
