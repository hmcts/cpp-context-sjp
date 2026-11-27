package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantDetailsUpdatesAcknowledgedProcessor {

    static final String PUBLIC_SJP_EVENTS_DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED = "public.sjp.defendant-details-updates-acknowledged";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles(DefendantDetailsUpdatesAcknowledged.EVENT_NAME)
    public void publish(final JsonEnvelope jsonEnvelope) {
        final JsonObject eventPayload = jsonEnvelope.payloadAsJsonObject();

        sender.send(enveloper.withMetadataFrom(
                jsonEnvelope,
                PUBLIC_SJP_EVENTS_DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED)
                .apply(createObjectBuilder()
                        .add("caseId", eventPayload.get("caseId"))
                        .add("defendantId", eventPayload.get("defendantId"))
                        .build()));
    }
}
