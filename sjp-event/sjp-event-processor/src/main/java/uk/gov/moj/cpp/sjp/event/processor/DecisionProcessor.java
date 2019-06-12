package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
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

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("public.resulting.referenced-decisions-saved")
    public void referencedDecisionsSaved(final Envelope<ReferencedDecisionsSaved> envelope) {
        final ReferencedDecisionsSaved referencedDecisionsSaved = envelope.payload();
        final UUID caseId = referencedDecisionsSaved.getCaseId();
        caseStateService.caseCompleted(caseId, envelope.metadata());
        final UUID sjpSessionId = referencedDecisionsSaved.getSjpSessionId();

        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(envelope.metadata()), NULL);
        final JsonObject sjpSessionPayload = sjpService.getSessionDetails(sjpSessionId, emptyEnvelope);
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, emptyEnvelope);

        final PublicSjpResulted jsonEnvelopeForResults = buildJsonEnvelopeForCCResults(caseId, envelope, caseDetails, sjpSessionPayload);
        sender.send(enveloper.withMetadataFrom(envelopeFrom(Envelope.metadataFrom(envelope.metadata()), NULL),
                "public.sjp.case-resulted").
                apply(objectToJsonObjectConverter.convert(jsonEnvelopeForResults)));
        sender.send(enveloper.withMetadataFrom(envelopeFrom(Envelope.metadataFrom(envelope.metadata()), NULL), "sjp.command.complete-case").apply(createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build()));
    }

    private PublicSjpResulted buildJsonEnvelopeForCCResults(final UUID caseId, final Envelope<ReferencedDecisionsSaved> envelope, final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {
        return converter.convert(caseId, envelope, caseDetails, sjpSessionPayload);
    }

}
