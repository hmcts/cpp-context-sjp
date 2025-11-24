package uk.gov.moj.cpp.sjp.event.processor.utils.builders;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.processor.utils.JsonHelper;

import javax.json.JsonObject;

public class ApplicationDecisionSetAsideEnvelope {

    public static JsonEnvelope of(final ApplicationDecisionSetAside payload) {
        return EnvelopeFactory.createEnvelope(ApplicationDecisionSetAside.EVENT_NAME, JsonHelper.toJsonObject(payload));
    }

    public static JsonEnvelope of(final JsonObject payload) {
        return EnvelopeFactory.createEnvelope(ApplicationDecisionSetAside.EVENT_NAME, payload);
    }
}
