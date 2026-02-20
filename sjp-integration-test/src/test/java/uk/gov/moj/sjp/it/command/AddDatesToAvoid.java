package uk.gov.moj.sjp.it.command;

import static java.lang.String.format;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

public class AddDatesToAvoid {

    public static void addDatesToAvoid(final UUID caseId, final String datesToAvoid) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("datesToAvoid", datesToAvoid)
                .build();

        makePostCall(format("/cases/%s/dates-to-avoid", caseId),
                "application/vnd.sjp.add-dates-to-avoid+json",
                payload.toString(),
                Response.Status.ACCEPTED);
    }

}
