package uk.gov.moj.cpp.sjp.event.processor;


import static com.google.common.collect.Sets.newHashSet;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsStarted;
import uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseCompletedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseCompletedProcessor.class);
    private static final String CASE_RESULTS = "sjp.query.case-results";
    private static final String REQUEST_DELETE_DOCS = "sjp.command.request-delete-docs";

    private static final String CASE_DECISIONS = "caseDecisions";
    private static final String OFFENCES = "offences";
    private static final String RESULTS = "results";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Sender sender;

    @Inject
    private SjpService sjpService;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private ResultingToResultsConverter converter;


    @Handles(CaseCompleted.EVENT_NAME)
    public void handleCaseCompleted(final JsonEnvelope caseCompletedEnvelope) {
        final JsonObject caseCompletedPayload = caseCompletedEnvelope.payloadAsJsonObject();
        final CaseCompleted caseCompleted = jsonObjectToObjectConverter.convert(caseCompletedPayload, CaseCompleted.class);
        final UUID caseId = caseCompleted.getCaseId();

        requestDeleteDocs(caseCompletedEnvelope, caseId);

        final JsonEnvelope caseResultsResponse = getCaseResults(caseCompletedEnvelope, caseCompletedPayload);
        validateCaseResults(caseResultsResponse);

        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, envelopeFrom(metadataFrom(caseCompletedEnvelope.metadata()), NULL));

        final Set<UUID> sjpSessionIds = CollectionUtils.isNotEmpty(caseCompleted.getSessionIds()) ? caseCompleted.getSessionIds() : newHashSet();

        for (final UUID sjpSessionId : sjpSessionIds) {
            final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(caseCompletedEnvelope.metadata()), NULL);
            final JsonObject sjpSessionPayload = sjpService.getSessionDetails(sjpSessionId, emptyEnvelope);

            final PublicSjpResulted jsonEnvelopeForResults = buildJsonEnvelopeForCCResults(caseId, caseResultsResponse, caseDetails, sjpSessionPayload);
            LOGGER.info("publishing sjp.case-resulted for case {} and session {}", caseId, sjpSessionId);
            final Envelope<PublicSjpResulted> envelope = envelop(
                    jsonEnvelopeForResults)
                    .withName("public.sjp.case-resulted")
                    .withMetadataFrom(caseCompletedEnvelope);
            sender.send(envelope);
        }
    }

    private void validateCaseResults(final JsonEnvelope caseResultsResponse) {
        final JsonObject payload = caseResultsResponse.payloadAsJsonObject();
        if(!payload.containsKey(CASE_DECISIONS)) {
            throw new IllegalArgumentException("No case decisions");
        }

        final JsonArray caseDecisions = payload.getJsonArray(CASE_DECISIONS);
        if(caseDecisions.isEmpty()) {
            throw new IllegalArgumentException("Case results. No case decisions");
        }

        final boolean anyEmptyOffenceDecision = caseDecisions.getValuesAs(JsonObject.class)
                .stream()
                .anyMatch(this::isACaseDecisionWithoutOffences);

        if(anyEmptyOffenceDecision) {
            throw new IllegalArgumentException("Case results. Case decision without offences");
        }

        final boolean anyOffenceWithoutResults = caseDecisions.getValuesAs(JsonObject.class)
                .stream()
                .flatMap(caseDecision -> caseDecision.getJsonArray(OFFENCES).getValuesAs(JsonObject.class).stream())
                .anyMatch(this::isAnOffenceWithoutResults);

        if(anyOffenceWithoutResults) {
            throw new IllegalArgumentException("Case results. Offence without results");
        }
    }

    private boolean isAnOffenceWithoutResults(final JsonObject offence) {
        return !offence.containsKey(RESULTS) || offence.getJsonArray(RESULTS).isEmpty();
    }

    private boolean isACaseDecisionWithoutOffences(final JsonObject caseDecision) {
        return !caseDecision.containsKey(OFFENCES) || caseDecision.getJsonArray(OFFENCES).isEmpty();
    }

    private void requestDeleteDocs(final JsonEnvelope caseCompletedEnvelope, final UUID caseId) {
        LOGGER.info("sending delete docs request for case {}", caseId);
        final JsonObject deleteDocsPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();
        final JsonEnvelope commandEnvelope = envelopeFrom(metadataFrom(caseCompletedEnvelope.metadata()).withName(REQUEST_DELETE_DOCS), deleteDocsPayload);
        sender.send(commandEnvelope);
    }

    private JsonEnvelope getCaseResults(final JsonEnvelope caseCompletedEnvelope, final JsonObject caseCompletedPayload) {
        final JsonEnvelope caseResultsRequestEnvelope = envelopeFrom(metadataFrom(caseCompletedEnvelope.metadata()).withName(CASE_RESULTS), caseCompletedPayload);
        return requester.request(caseResultsRequestEnvelope);
    }

    @Handles(FinancialMeansDeleteDocsStarted.EVENT_NAME)
    public void handleDeleteDocsStarted(final JsonEnvelope deleteDocsStartedEnvelope) {
        final JsonObject deleteDocsStartedPayload = deleteDocsStartedEnvelope.payloadAsJsonObject();
        final FinancialMeansDeleteDocsStarted deleteDocsStarted = jsonObjectToObjectConverter.convert(deleteDocsStartedPayload, FinancialMeansDeleteDocsStarted.class);
        sender.send(envelop(
                createObjectBuilder()
                        .add(CASE_ID, deleteDocsStarted.getCaseId().toString())
                        .add(DEFENDANT_ID, deleteDocsStarted.getDefendantId().toString())
                        .build())
                .withName("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn")
                .withMetadataFrom(deleteDocsStartedEnvelope));
    }

    private PublicSjpResulted buildJsonEnvelopeForCCResults(final UUID caseId, final Envelope caseResultsResponse,
                                                            final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {
        return converter.convert(caseId, caseResultsResponse, caseDetails, sjpSessionPayload);
    }
}
