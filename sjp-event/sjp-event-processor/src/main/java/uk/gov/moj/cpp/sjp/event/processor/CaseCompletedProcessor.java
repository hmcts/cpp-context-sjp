package uk.gov.moj.cpp.sjp.event.processor;


import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
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
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.resulting.CaseResults;
import uk.gov.moj.cpp.sjp.domain.resulting.Offence;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String[] WITHDRAWN_OR_DISMISSED_SHORTCODES = {"DISM", "WDRNNOT"};
    private static final String FIELD_SHORT_CODE = "shortCode";
    private static final String FIELD_ID = "id";
    private static final JsonObject EMPTY_JSON_OBJECT = createObjectBuilder().build();
    @Inject
    private Enveloper enveloper;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private Sender sender;
    @Inject
    private SjpService sjpService;
    @Inject
    private ReferenceDataService referenceDataService;
    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;
    @Inject
    private ResultingToResultsConverter converter;


    @Handles(CaseCompleted.EVENT_NAME)
    public void handleCaseCompleted(final JsonEnvelope caseCompletedEnvelope) {

        final JsonObject payloadAsJsonObject = caseCompletedEnvelope.payloadAsJsonObject();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(caseCompletedEnvelope.metadata()).withName(CASE_RESULTS), payloadAsJsonObject);
        final CaseCompleted caseCompleted = jsonObjectToObjectConverter.convert(payloadAsJsonObject, CaseCompleted.class);

        final Envelope<?> caseResultsResponse = requester.request(requestEnvelope);

        LOGGER.info("Received case results : {}", caseResultsResponse.payload());
        final CaseResults caseResults = jsonObjectToObjectConverter.convert((JsonObject) caseResultsResponse.payload(), CaseResults.class);

        final UUID caseId = caseResults.getCaseId();
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, envelopeFrom(
                metadataFrom(caseCompletedEnvelope.metadata()), NULL));
        final UUID defendantId = caseDetails.getDefendant().getId();

        final boolean allOffencesWithDrawnOrDismissed =
                caseResults
                        .getCaseDecisions()
                        .stream()
                        .allMatch(caseDecision -> !caseDecision.getOffences().isEmpty() &&
                                TRUE.equals(
                                        allOffencesWithdrawnOrDismissed(caseDecision.getOffences(),
                                                getWithdrawnOrDismissedResultDefinitions(
                                                        caseCompletedEnvelope.metadata(),
                                                        caseDecision.getResultedOn().toLocalDate()))));

        if (allOffencesWithDrawnOrDismissed) {
            sender.send(envelop(
                    createObjectBuilder()
                            .add(CASE_ID, caseId.toString())
                            .add(DEFENDANT_ID, defendantId.toString())
                            .build())
                    .withName("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn")
                    .withMetadataFrom(caseCompletedEnvelope));
        }

        final Set<UUID> sjpSessionIds = CollectionUtils.isNotEmpty(caseCompleted.getSessionIds()) ? caseCompleted.getSessionIds() : newHashSet();

        for (final UUID sjpSessionId : sjpSessionIds) {
            final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(caseCompletedEnvelope.metadata()), NULL);
            final JsonObject sjpSessionPayload = sjpService.getSessionDetails(sjpSessionId, emptyEnvelope);

            final PublicSjpResulted jsonEnvelopeForResults = buildJsonEnvelopeForCCResults(caseId, caseResultsResponse, caseDetails, sjpSessionPayload);

            final Envelope<PublicSjpResulted> envelope = envelop(
                    jsonEnvelopeForResults)
                    .withName("public.sjp.case-resulted")
                    .withMetadataFrom(caseCompletedEnvelope);
            sender.send(envelope);
        }

    }

    private PublicSjpResulted buildJsonEnvelopeForCCResults(final UUID caseId, final Envelope envelope, final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {
        return converter.convert(caseId, envelope, caseDetails, sjpSessionPayload);
    }

    /* Assume all offences are withdrawn or dismissed and then check each result for each offence*/
    private Boolean allOffencesWithdrawnOrDismissed(final List<Offence> offences, final Map<UUID, String> withdrawnOrDismissedResultDefinitions) {
        if (isEmpty(offences)) {
            throw new IllegalArgumentException("No offences in ReferencedDecisionsSaved event");
        }
        for (final Offence offence : offences) {
            if (!areAllResultsDismissedOrWithdrawn(offence.getResults(), withdrawnOrDismissedResultDefinitions)) {
                return FALSE;
            }
        }
        return TRUE;
    }

    private Boolean areAllResultsDismissedOrWithdrawn(final List<Result> results, final Map<UUID, String> withdrawnOrDismissedResultDefinitions) {
        if (isEmpty(results)) {
            throw new IllegalArgumentException("No results for offence in ReferencedDecisionsSaved event");
        }
        for (final Result result : results) {
            if (!(withdrawnOrDismissed(result, withdrawnOrDismissedResultDefinitions))) {
                return FALSE;
            }
        }
        return TRUE;
    }

    private boolean withdrawnOrDismissed(final Result result, final Map<UUID, String> withdrawnOrDismissedResultDefinitions) {
        return withdrawnOrDismissedResultDefinitions.keySet().contains(result.getResultDefinitionId());
    }

    private Map<UUID, String> getWithdrawnOrDismissedResultDefinitions(final Metadata metadata, final LocalDate onDate) {

        final Map<UUID, String> withdrawnOrDismissedResultDefinitions = new HashMap<>();
        final JsonArray allResultDefinitions = referenceDataService.getAllResultDefinitions(envelopeFrom(metadata, EMPTY_JSON_OBJECT), onDate);

        for (final JsonObject resultDefinition : allResultDefinitions.getValuesAs(JsonObject.class)) {
            if (withdrawnOrDismissedResultDefinition(resultDefinition)) {
                withdrawnOrDismissedResultDefinitions.put(fromString(resultDefinition.getString(FIELD_ID)), resultDefinition.getString(FIELD_SHORT_CODE));
            }
        }
        return withdrawnOrDismissedResultDefinitions;
    }

    private boolean withdrawnOrDismissedResultDefinition(final JsonObject resultDefinition) {
        return Arrays.asList(WITHDRAWN_OR_DISMISSED_SHORTCODES).contains(resultDefinition.getString(FIELD_SHORT_CODE));
    }
}
