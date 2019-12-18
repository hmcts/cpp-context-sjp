package uk.gov.moj.cpp.sjp.query.api.helper;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.json.JsonObject;

public class JsonHelper {
    private JsonHelper() {
        
    }
    public static Optional<JsonObject> getPayload(JsonEnvelope envelope) {
        return Optional.of(envelope.payload())
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast);
    }

}
