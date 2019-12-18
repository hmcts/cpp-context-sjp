package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready;

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
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service.SjpViewStoreService;

import java.util.stream.Stream;

import javax.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class CaseMarkedReadyForDecisionEventTransformer implements EventTransformation {

    private SjpViewStoreService sjpViewStoreService;
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseMarkedReadyForDecisionEventTransformer.class);
    static final String CASE_MARKED_READY_FOR_DECISION_EVENT_NAME = "sjp.events.case-marked-ready-for-decision";
    static final String CASE_ID = "caseId";
    static final String REASON = "reason";
    static final String SESSION_TYPE = "sessionType";
    static final String PRIORITY = "priority";
    static final String PRIORITY_VALUE = "MEDIUM";

    public CaseMarkedReadyForDecisionEventTransformer() {
        this(SjpViewStoreService.getInstance());
    }

    @VisibleForTesting
    CaseMarkedReadyForDecisionEventTransformer(final SjpViewStoreService sjpViewStoreService) {
        this.sjpViewStoreService = sjpViewStoreService;
    }

    @Override
    public void setEnveloper(Enveloper enveloper) {
        // Nothing to do here
    }

    @Override
    public Action actionFor(JsonEnvelope event) {

        boolean isCaseMarkedReadyForDecisionEvent = CASE_MARKED_READY_FOR_DECISION_EVENT_NAME.equals(event.metadata().name());

        if (!isCaseMarkedReadyForDecisionEvent) {
            return NO_ACTION;
        }

        final String caseId = event.asJsonObject().getString(CASE_ID);

        boolean forMigration = sjpViewStoreService.getWhetherCaseIsCandidateForMigration(caseId);

        if (!forMigration) {
            return NO_ACTION;
        }

        return TRANSFORM;
    }

    @Override
    public Stream<JsonEnvelope> apply(JsonEnvelope existingEvent) {

        final String caseId = existingEvent.asJsonObject().getString(CASE_ID);
        final String reason = existingEvent.asJsonObject().getString(REASON);

        final JsonObject eventPayload = existingEvent.payloadAsJsonObject();

        if (reason == null || reason.trim().isEmpty()) {
            LOGGER.warn("Case with Id :{} does not have a record in the ready_cases table and event sjp.events.case-marked-ready-for-decision has not been migrated for this case", caseId);
            throw new TransformationException(String.format("Case with Id :%s does not have a record in the ready_cases table and event sjp.events.case-marked-ready-for-decision has not been migrated for this case", caseId));
        }

        final JsonObject transformedEvent = transformEvent(eventPayload, reason);

        final JsonEnvelope caseMarkedReadyForDecisionEvent = envelopeFrom(metadataFrom(existingEvent.metadata()), transformedEvent);

        return Stream.of(caseMarkedReadyForDecisionEvent);
    }

    private JsonObject transformEvent(JsonObject eventPayload, String reason) {
        CaseReadinessReason caseReadinessReason = CaseReadinessReason.valueOf(reason);
        SessionType sessionType = getSessionType(caseReadinessReason);

        return JsonObjects.createObjectBuilder(eventPayload)
                .add(SESSION_TYPE, sessionType.name())
                .add(PRIORITY, PRIORITY_VALUE)
                .build();
    }

    private static SessionType getSessionType(final CaseReadinessReason readinessReason) {
        final SessionType sessionType;
        switch (readinessReason) {
            case PIA:
            case PLEADED_GUILTY:
                sessionType = SessionType.MAGISTRATE;
                break;
            default:
                sessionType = SessionType.DELEGATED_POWERS;
        }
        return sessionType;
    }
}
