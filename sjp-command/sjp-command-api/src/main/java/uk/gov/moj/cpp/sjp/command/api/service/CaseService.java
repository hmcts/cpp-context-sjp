package uk.gov.moj.cpp.sjp.command.api.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CaseService {

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public JsonObject getCaseDetails(final JsonEnvelope envelope){
        final JsonObject payload = createObjectBuilder().add("caseId", envelope.payloadAsJsonObject().getString("caseId")).build();

        final JsonEnvelope queryCaseEnvelope = JsonEnvelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.query.case").build(),
                payload);

        return requester.request(queryCaseEnvelope).payloadAsJsonObject();
    }

}
