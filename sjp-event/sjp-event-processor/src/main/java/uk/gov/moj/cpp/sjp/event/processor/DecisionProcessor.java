package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DecisionProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private ResultingToResultsConverter converter;

    @Inject
    private Sender sender;

    @Inject
    private CaseStateService caseStateService;

    @Inject
    private SjpService sjpService;


    @Handles("public.resulting.referenced-decisions-saved")
    public void referencedDecisionsSaved(final JsonEnvelope envelope) {
        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        caseStateService.caseCompleted(caseId, envelope.metadata());
        final UUID sjpSessionId = fromString(envelope.payloadAsJsonObject().getString("sjpSessionId"));

        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(envelope.metadata()), NULL);
        final JsonObject sjpSessionPayload = sjpService.getSessionDetails(sjpSessionId, emptyEnvelope);
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, emptyEnvelope);

        final JsonObject jsonEnvelopeForResults = buildJsonEnvelopeForCCResults(caseId, envelope, caseDetails, sjpSessionPayload);
        sender.send(enveloper.withMetadataFrom(envelope,
                "public.sjp.case-resulted").
                apply(jsonEnvelopeForResults));
    }

    private JsonObject buildJsonEnvelopeForCCResults(final UUID caseId, final JsonEnvelope envelope, final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {
        return converter.convert(caseId, envelope, caseDetails, sjpSessionPayload);
    }

}
