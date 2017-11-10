package uk.gov.moj.cpp.sjp.command.service;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;

/**
 * Used for querying Case Decision from Resulting Context.
 */
public class ResultingQueryService {

    @ServiceComponent(Component.COMMAND_CONTROLLER)
    @Inject
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public JsonEnvelope findCaseDecision(final JsonEnvelope envelope) {

        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(envelope, "resulting.query.case-decisions")
                .apply(Json.createObjectBuilder()
                        .add("caseId", envelope.payloadAsJsonObject().getString("caseId"))
                        .build());
        return requester.request(requestEnvelope);
    }
}
