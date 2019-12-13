package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.service.ResultingService;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.service.ReferenceDecisionSavedResult;

import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class CaseReferredForCourtHearingEventTransformer implements EventTransformation {

    private ResultingService resultingService;
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReferredForCourtHearingEventTransformer.class);
    public static final String CASE_REFERRED_FOR_COURT_HEARING_EVENT_NAME = "sjp.events.case-referred-for-court-hearing";
    public static final String CASE_ID = "caseId";
    public static final String DECISION_ID = "decisionId";
    public static final String ESTIMATED_HEARING_DURATION = "estimatedHearingDuration";
    public static final String LISTING_NOTES = "listingNotes";
    public static final String REFERRAL_REASON_ID = "referralReasonId";
    public static final String REFERRED_AT = "referredAt";
    public static final String OFFENCE_ID = "offenceId";
    public static final String VERDICT = "verdict";
    public static final String REFERRED_OFFENCES = "referredOffences";

    public CaseReferredForCourtHearingEventTransformer() {
        this.resultingService = ResultingService.getInstance();
    }

    @VisibleForTesting
    public CaseReferredForCourtHearingEventTransformer(final ResultingService resultingService ) {
        this.resultingService = resultingService;
    }

    @Override
    public void setEnveloper(Enveloper enveloper) {
        // Nothing to do here
    }

    @Override
    public Action actionFor(JsonEnvelope event) {

        boolean isCaseReferredForCourtHearingEvent = CASE_REFERRED_FOR_COURT_HEARING_EVENT_NAME.equals(event.metadata().name());

        if (!isCaseReferredForCourtHearingEvent) {
            return NO_ACTION;
        }

        if (getValueOrNull(event, DECISION_ID) != null) {
            return NO_ACTION;
        }

        return TRANSFORM;
    }

    @Override
    @SuppressWarnings("squid:S2139")
    public Stream<JsonEnvelope> apply(JsonEnvelope existingEvent) {
        final String caseId = existingEvent.asJsonObject().getString(CASE_ID);
        try {
            return doApply(existingEvent);
        } catch(TransformationException e) {
            LOGGER.error("Apply failed transformation of caseId {}", caseId, e);
            throw e;
        } catch(RuntimeException e) {
            LOGGER.error("Apply failed with generic error of caseId {}", caseId, e);
            throw e;
        }
    }

    private Stream<JsonEnvelope> doApply(JsonEnvelope existingEvent) {

        final String caseId = existingEvent.asJsonObject().getString(CASE_ID);
        final int estimatedHearingDuration = existingEvent.asJsonObject().getInt(ESTIMATED_HEARING_DURATION);
        final String listingNotes = getValueOrNull(existingEvent, LISTING_NOTES);
        final String referralReasonId = existingEvent.asJsonObject().getString(REFERRAL_REASON_ID);
        final String referredAt = existingEvent.asJsonObject().getString(REFERRED_AT);

        LOGGER.info("Start of transform for event sjp.events.case-referred-for-court-hearing with case Id : {}", caseId);

        ReferenceDecisionSavedResult referenceDecisionSavedResult = resultingService.getReferencedDecisionSavedEventByCaseId(caseId);

        if (referenceDecisionSavedResult == null) {
            LOGGER.warn("Case with Id :{} does not have an event for sjp.events.case-referred-for-court-hearing in the event_log table of resulting and this event has not been migrated for this case", caseId);
            throw new TransformationException(String.format("Case with Id :%s does not have an event for sjp.events.case-referred-for-court-hearing in the event_log table of resulting and this event has not been migrated for this case", caseId));
        }

        LOGGER.info("Transforming case {}", caseId);

        final JsonObject transformedEvent = transformEvent(caseId, estimatedHearingDuration, referralReasonId, referredAt, listingNotes, referenceDecisionSavedResult);

        final JsonEnvelope caseReferredForCourtHearingEvent = envelopeFrom(metadataFrom(existingEvent.metadata()), transformedEvent);

        return Stream.of(caseReferredForCourtHearingEvent);
    }

    private JsonObject transformEvent(String caseId, int estimatedHearingDuration, String referralReasonId, String referredAt, String listingNotes, ReferenceDecisionSavedResult referenceDecisionSavedResult) {

        JsonObject payload = referenceDecisionSavedResult.getPayload();
        JsonArray offences = payload.getJsonArray("offences");
        String offenceId = offences.getJsonObject(0).getString("id");
        final String decisionId = payload.getString("id");
        final String verdict = payload.getString(VERDICT);

        final JsonArrayBuilder referredOffencesBuilder = Json.createArrayBuilder();

        referredOffencesBuilder.add(createObjectBuilder()
                .add(OFFENCE_ID, offenceId)
                .add(VERDICT, mapVerdict(verdict)));

        JsonObject transformedEvent = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(REFERRED_OFFENCES, referredOffencesBuilder)
                .add(ESTIMATED_HEARING_DURATION, estimatedHearingDuration)
                .add(REFERRAL_REASON_ID, referralReasonId)
                .add(REFERRED_AT, referredAt)
                .add(DECISION_ID, decisionId)
                .build();

        if (listingNotes == null || listingNotes.isEmpty()) {
            return transformedEvent;
        }

        return JsonObjects.createObjectBuilder(transformedEvent)
                .add(LISTING_NOTES, listingNotes)
                .build();
    }

    private String mapVerdict(final String verdict) {

        switch(verdict) {
            case "FNG":
                return VerdictType.FOUND_NOT_GUILTY.name();
            case "PSJ":
                return VerdictType.PROVED_SJP.name();
            case "GSJ":
                return VerdictType.FOUND_GUILTY.name();
            default:
                return VerdictType.NO_VERDICT.name();
        }
    }

    private String getValueOrNull(JsonEnvelope jsonEnvelope, final String propertyName) {
        if (!jsonEnvelope.asJsonObject().containsKey(propertyName)) {
            return null;
        }

        return jsonEnvelope.asJsonObject().getString(propertyName);
    }
}
