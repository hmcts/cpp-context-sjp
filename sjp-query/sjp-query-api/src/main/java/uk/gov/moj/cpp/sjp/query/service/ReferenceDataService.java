package uk.gov.moj.cpp.sjp.query.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.exception.OffenceNotFoundException;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    private static final String FINE_LEVELS = "fineLevels";

    public JsonObject getOffenceDefinition(final String offenceCode, final String date, final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(envelope, "referencedataoffences.query.offences-list")
                .apply(createObjectBuilder()
                        .add("cjsoffencecode", offenceCode)
                        .add("date", date)
                        .build());
        final JsonEnvelope response = requester.request(request);
        return response.payloadAsJsonObject()
                .getJsonArray("offences")
                .getValuesAs(JsonObject.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new OffenceNotFoundException(offenceCode));
    }

    public List<JsonObject> getReferralReasons(final JsonEnvelope jsonEnvelope) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(jsonEnvelope, "referencedata.query.referral-reasons")
                .apply(createObjectBuilder().build());

        return requester.requestAsAdmin(request)
                .payloadAsJsonObject()
                .getJsonArray("referralReasons")
                .getValuesAs(JsonObject.class);
    }

    public List<JsonObject> getOffenceFineLevels(final JsonEnvelope jsonEnvelope) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(jsonEnvelope, "referencedata.query.offence-fine-levels")
                .apply(createObjectBuilder().build());

        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payloadAsJsonObject().getJsonArray(FINE_LEVELS).getValuesAs(JsonObject.class);
    }

    public List<JsonObject> getWithdrawalReasons(final JsonEnvelope jsonEnvelope) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(jsonEnvelope, "referencedata.query.offence-withdraw-request-reasons")
                .apply(createObjectBuilder().build());

        return requester.requestAsAdmin(request)
                .payloadAsJsonObject()
                .getJsonArray("offenceWithdrawRequestReasons")
                .getValuesAs(JsonObject.class);
    }
}
