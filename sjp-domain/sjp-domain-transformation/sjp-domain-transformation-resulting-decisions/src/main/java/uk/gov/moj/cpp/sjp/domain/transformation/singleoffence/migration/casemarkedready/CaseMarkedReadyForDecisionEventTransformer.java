package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready;

import static java.time.temporal.ChronoUnit.DAYS;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpEventStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class CaseMarkedReadyForDecisionEventTransformer implements EventTransformation {

    public static final String ID = "id";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseMarkedReadyForDecisionEventTransformer.class);
    static final String CASE_MARKED_READY_FOR_DECISION_EVENT_NAME = "sjp.events.case-marked-ready-for-decision";
    static final String CASE_ID = "caseId";
    static final String REASON = "reason";
    static final String SESSION_TYPE = "sessionType";
    static final String PRIORITY = "priority";
    static final String PRIORITY_VALUE = "MEDIUM";
    private static final long NOTICE_PERIOD  = 28;
    private static final String NAME = "name";
    public static final String SJP_EVENTS_DEFENDANT_RESPONSE_TIMER_EXPIRED = "sjp.events.defendant-response-timer-expired";

    private SjpViewStoreService sjpViewStoreService;
    private SjpEventStoreService sjpEventStoreService;

    public CaseMarkedReadyForDecisionEventTransformer() {
        this(SjpViewStoreService.getInstance(),
                SjpEventStoreService.getInstance());
    }

    @VisibleForTesting
    CaseMarkedReadyForDecisionEventTransformer(final SjpViewStoreService sjpViewStoreService,
                                               final SjpEventStoreService sjpEventStoreService) {
        this.sjpViewStoreService = sjpViewStoreService;
        this.sjpEventStoreService = sjpEventStoreService;
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
        final String reason = existingEvent.asJsonObject().getString(REASON);
        final String metaDataId = existingEvent.metadata().id().toString();

        final JsonObject eventPayload = existingEvent.payloadAsJsonObject();

        if (reason == null || reason.trim().isEmpty()) {
            LOGGER.warn("Case with Id :{} does not have a record in the ready_cases table and event sjp.events.case-marked-ready-for-decision has not been migrated for this case", caseId);
            throw new TransformationException(String.format("Case with Id :%s does not have a record in the ready_cases table and event sjp.events.case-marked-ready-for-decision has not been migrated for this case", caseId));
        }

        LOGGER.info("Transforming case {}", caseId);

        final JsonObject transformedEvent = transformEvent(eventPayload, reason);

        final JsonEnvelope caseMarkedReadyForDecisionEvent = envelopeFrom(metadataFrom(existingEvent.metadata()), transformedEvent);

        final Stream.Builder<JsonEnvelope> finalStreamBuilder = Stream.builder();
        final Optional<LocalDate> postingDateOptional = sjpViewStoreService.getPostingDate(caseId);
        // create a response time expired where applicable and only if the case is in ready_cases
        postingDateOptional
                .ifPresent(postingDate -> {
                    final Optional<JsonEnvelope> expiredEvent = getResponseTimeExpiredEvent(existingEvent, caseId, metaDataId, postingDate);
                    expiredEvent.ifPresent(event -> finalStreamBuilder.add(event));
                });

        finalStreamBuilder.add(caseMarkedReadyForDecisionEvent);
        return finalStreamBuilder.build();
    }

    private Optional<JsonEnvelope> getResponseTimeExpiredEvent(final JsonEnvelope envelope,
                                                               final String caseId,
                                                               final String metaDataId,
                                                               final LocalDate postingDate) {
        // check the response time expired
        if (isCaseNoticePeriodExpired(postingDate)
                && metaDataId.equals(getMetadataIdOfTheFirstEligibleReadyForDecisionDueTo28DaysTimer(caseId, postingDate))) {
            final JsonObjectBuilder metadataJsonObjectBuilder = createObjectBuilderWithFilter(envelope.metadata().asJsonObject(),
                    field -> !NAME.equalsIgnoreCase(field));
            metadataJsonObjectBuilder.add(ID, UUID.randomUUID().toString());
            metadataJsonObjectBuilder.add(NAME, SJP_EVENTS_DEFENDANT_RESPONSE_TIMER_EXPIRED);
            return Optional.of(envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), createDefendantResponseTimerExpired(caseId)));
        }

        return Optional.empty();
    }

    private String getMetadataIdOfTheFirstEligibleReadyForDecisionDueTo28DaysTimer(final String caseId,
                                                                   final LocalDate postingDate) {
        // find the first potential marked ready for decision event due to 28 days timer
        final List<CaseMarkedReadyReadyMetaData> metaDataList =
                sjpEventStoreService.getMarkedReadyForDecisionMetadata(caseId);

        final Optional<CaseMarkedReadyReadyMetaData> firstEligibleOne =
                metaDataList.stream()
                        .filter((e) -> hasExpiredOnThisMarkedDecision(postingDate, e.getMarkedAt()))
                        .findFirst();

        return firstEligibleOne
                .map(e -> e.getMetaDataId())
                .orElse(null);
    }

    private JsonObject transformEvent(JsonObject eventPayload, String reason) {
        CaseReadinessReason caseReadinessReason = CaseReadinessReason.valueOf(reason);
        SessionType sessionType = getSessionType(caseReadinessReason);

        return JsonObjects.createObjectBuilder(eventPayload)
                .add(SESSION_TYPE, sessionType.name())
                .add(PRIORITY, PRIORITY_VALUE)
                .build();
    }

    private JsonObject createDefendantResponseTimerExpired(final String caseId) {
        return createObjectBuilder()
                .add("caseId", caseId)
                .build();
    }

    private boolean isCaseNoticePeriodExpired(final LocalDate postingDate) {
        final LocalDate noticeEndDate = postingDate.plusDays(NOTICE_PERIOD);
        return LocalDate.now().isAfter(noticeEndDate.minus(1, DAYS));
    }

    private boolean hasExpiredOnThisMarkedDecision(final LocalDate postingDate,
                                                   final LocalDate markedDate) {
        final LocalDate noticeEndDate = postingDate.plusDays(NOTICE_PERIOD);
        return markedDate.isAfter(noticeEndDate.minus(1, DAYS));
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
