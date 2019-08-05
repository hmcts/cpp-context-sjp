package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.resulting.Offence;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DecisionProcessor {

    private static final String[] WITHDRAWN_OR_DISMISSED_SHORTCODES = {"DISM", "WDRNNOT"};
    private static final String FIELD_SHORT_CODE = "shortCode";
    private static final String FIELD_ID = "id";

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
    private ReferenceDataService referenceDataService;
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

        generateEventWhereAllOffencesDismissedOrWithdrawn(envelope, referencedDecisionsSaved.getOffences(), caseDetails);

        sender.send(enveloper.withMetadataFrom(envelopeFrom(Envelope.metadataFrom(envelope.metadata()), NULL), "sjp.command.complete-case").apply(createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build()));
    }

    private PublicSjpResulted buildJsonEnvelopeForCCResults(final UUID caseId, final Envelope<ReferencedDecisionsSaved> envelope, final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {
        return converter.convert(caseId, envelope, caseDetails, sjpSessionPayload);
    }

    private void generateEventWhereAllOffencesDismissedOrWithdrawn(final Envelope<ReferencedDecisionsSaved> envelope, final List<Offence> offences, final CaseDetails caseDetails) {
        final Map<UUID, String> withdrawnOrDismissedResultDefinitions = getWithdrawnOrDismissedResultDefinitions(envelope.metadata());
        if (areAllOffencesDismissedOrWithdrawn(offences, withdrawnOrDismissedResultDefinitions)) {
            sendPublicEventAllOffencesDismissedOrWithdrawn(envelope, caseDetails);
        }
    }

    /* Assume all offences are withdrawn or dismissed and then check each result for each offence*/
    private Boolean areAllOffencesDismissedOrWithdrawn(final List<Offence> offences, final Map<UUID, String> withdrawnOrDismissedResultDefinitions) {
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

    private void sendPublicEventAllOffencesDismissedOrWithdrawn(final Envelope<ReferencedDecisionsSaved> envelope, final CaseDetails caseDetails) {
        sender.send(Enveloper.envelop(createObjectBuilder()
                .add(CASE_ID, caseDetails.getId().toString())
                .add(DEFENDANT_ID, caseDetails.getDefendant().getId().toString())
                .build()).withName("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn").withMetadataFrom(envelope));
    }

    private Map<UUID, String> getWithdrawnOrDismissedResultDefinitions(final Metadata metadata) {

        final Map<UUID, String> withdrawnOrDismissedResultDefinitions = new HashMap<>();
        final JsonArray allResultDefinitions = referenceDataService.getAllResultDefinitions(envelopeFrom(metadata, createObjectBuilder().build()));

        for (final JsonObject resultDefinition : allResultDefinitions.getValuesAs(JsonObject.class)) {
            if (withdrawnOrDismissedResultDefinition(resultDefinition)) {
                withdrawnOrDismissedResultDefinitions.put(UUID.fromString(resultDefinition.getString(FIELD_ID)), resultDefinition.getString(FIELD_SHORT_CODE));
            }
        }
        return withdrawnOrDismissedResultDefinitions;
    }

    private boolean withdrawnOrDismissedResultDefinition(final JsonObject resultDefinition) {
        return Arrays.asList(WITHDRAWN_OR_DISMISSED_SHORTCODES).contains(resultDefinition.getString(FIELD_SHORT_CODE));
    }

}
