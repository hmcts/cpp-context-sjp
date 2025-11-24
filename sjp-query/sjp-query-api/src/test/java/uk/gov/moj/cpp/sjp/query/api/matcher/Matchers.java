package uk.gov.moj.cpp.sjp.query.api.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;

import java.util.UUID;

public class Matchers {
    public static JsonEnvelopeMatcher materialMetadataRequest(final JsonEnvelope sourceEnvelope, final UUID materialId) {
        return jsonEnvelope(withMetadataEnvelopedFrom(sourceEnvelope).withName("material.query.material-metadata"),
                payloadIsJson(withJsonPath("$.materialId", equalTo(materialId.toString()))));
    }

}
