package uk.gov.moj.cpp.sjp.event.processor.utils.builders;

import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.event.processor.utils.JsonHelper;

public class ApplicationDecisionSavedEnvelope {

    private static final String COMMAND_NAME = "sjp.events.application-decision-saved";

    public static JsonEnvelope of(final ApplicationDecisionSaved payload) {
        return EnvelopeFactory.createEnvelope(COMMAND_NAME, JsonHelper.toJsonObject(payload));
    }
}
