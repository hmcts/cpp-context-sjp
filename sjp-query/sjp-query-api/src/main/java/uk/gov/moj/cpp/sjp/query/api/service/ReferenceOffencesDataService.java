package uk.gov.moj.cpp.sjp.query.api.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class ReferenceOffencesDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public JsonObject getOffenceReferenceData(final JsonEnvelope envelope, final String offenceCode, final String date) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(envelope, "referencedataoffences.query.offences-list")
                .apply(Json.createObjectBuilder()
                        .add("cjsoffencecode", offenceCode)
                        .add("date", date)
                        .build());
        final JsonEnvelope response = requester.request(request);
        return response.payloadAsJsonObject().getJsonArray("offences").getJsonObject(0);
    }

    public JsonObject getOffenceReferenceDataByOffenceId(final JsonEnvelope envelope, final String offenceId) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(envelope, "referencedataoffences.query.offence")
                .apply(Json.createObjectBuilder()
                        .add("offenceId", offenceId)
                        .build());
        final JsonEnvelope response = requester.request(request);
        return response.payloadAsJsonObject();
    }
}
