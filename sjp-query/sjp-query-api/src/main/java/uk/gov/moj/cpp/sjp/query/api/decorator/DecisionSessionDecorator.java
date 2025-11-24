package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.service.UsersGroupsService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class DecisionSessionDecorator {

    @Inject
    private UsersGroupsService usersGroupsService;

    public JsonObject decorateWithLegalAdviserName(final JsonObject caseView, final JsonEnvelope envelope) {

        final JsonArray caseDecisions = caseView.getJsonArray("caseDecisions");

        if (caseDecisions == null) {
            return caseView;
        } else {
            return createObjectBuilder(caseView)
                    .add("caseDecisions", decorateDecisions(caseDecisions, envelope))
                    .build();
        }
    }

    private JsonArray decorateDecisions(final JsonArray decisions, final JsonEnvelope envelope) {

        final JsonArrayBuilder decisionsBuilder = Json.createArrayBuilder();

        decisions.getValuesAs(JsonObject.class).forEach(decision -> {

            final JsonObjectBuilder decisionBuilder = createObjectBuilder(decision);
            final JsonObject session = decision.getJsonObject("session");

            if (session != null) {
                decisionBuilder.add("session", decorateSession(session, envelope));
            }
            decisionsBuilder.add(decisionBuilder);
        });

        return decisionsBuilder.build();
    }

    private JsonObject decorateSession(final JsonObject session, final JsonEnvelope envelope) {

        final UUID legalAdviserUserId = fromString(session.getString("legalAdviserUserId"));

        final JsonObject legalAdviserDetails = usersGroupsService.getUserDetails(
                legalAdviserUserId, envelope);

        return createObjectBuilder(session)
                .add("legalAdviser", Json.createObjectBuilder()
                        .add("id", legalAdviserUserId.toString())
                        .add("firstName", legalAdviserDetails.getString("firstName"))
                        .add("lastName", legalAdviserDetails.getString("lastName"))
                )
                .build();
    }
}
