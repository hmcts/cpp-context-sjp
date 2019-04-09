package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.MonthDay.now;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DecisionProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private CaseStateService caseStateService;

    @Handles("public.resulting.referenced-decisions-saved")
    public void referencedDecisionsSaved(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        caseStateService.caseCompleted(caseId, envelope.metadata());

        final JsonObject payload =  createObjectBuilder().add("caseId", caseId.toString())
                .add("hearing", "")
                .add("variants", "")
                .add("sharedTime", now().toString())
                .build();

        final JsonEnvelope command = enveloper.withMetadataFrom(envelopeFrom(metadataFrom(envelope.metadata()),NULL), "public.sjp.case-resulted")
                .apply(payload);

        sender.send(command);
    }

}
