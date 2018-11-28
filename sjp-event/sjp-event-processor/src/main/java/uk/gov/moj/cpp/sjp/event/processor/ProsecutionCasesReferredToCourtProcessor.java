package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCasesReferredToCourtProcessor {

    public static final String EVENT_NAME = "public.progression.prosecution-cases-referred-to-court";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles(ProsecutionCasesReferredToCourtProcessor.EVENT_NAME)
    public void handleProsecutionCasesReferredToCourtEvent(final JsonEnvelope prosecutionCasesReferredToCourtEvent) {

        sender.send(enveloper.withMetadataFrom(prosecutionCasesReferredToCourtEvent, "sjp.command.update-case-listed-in-criminal-courts").
                apply(createObjectBuilder()
                        .add("caseId", prosecutionCasesReferredToCourtEvent.payloadAsJsonObject().getString("prosecutionCaseId"))
                        .build()));
    }
}
