package uk.gov.moj.cpp.sjp.event.processor.service;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class ReferenceDataOffencesService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public JsonObject getOffences(final Offence offence, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("cjsoffencecode", offence.getCjsCode()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedataoffences.query.offences-list").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

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
}
