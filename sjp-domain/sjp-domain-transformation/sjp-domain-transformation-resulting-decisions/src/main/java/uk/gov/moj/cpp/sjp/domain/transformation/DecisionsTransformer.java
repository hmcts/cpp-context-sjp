package uk.gov.moj.cpp.sjp.domain.transformation;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.NAME;
import static uk.gov.moj.cpp.sjp.event.CaseCompleted.EVENT_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.converter.ResultingDecisionsConverter;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.service.ResultingService;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpEventStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.annotations.VisibleForTesting;

@Transformation
public class DecisionsTransformer implements EventTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionsTransformer.class);

    private static final String CASE_ID = "caseId";
    public static final String SJP_EVENTS_CASE_ADJOURNED_TO_LATER_SJP_HEARING_RECORDED = "sjp.events.case-adjourned-to-later-sjp-hearing-recorded";
    public static final String CASE_STATUS = "caseStatus";
    public static final String ID = "id";

    private ResultingService resultingService;
    private ResultingDecisionsConverter resultingDecisionsConverter;
    private SjpEventStoreService sjpEventStoreService;
    private SjpViewStoreService sjpViewStoreService;

    public DecisionsTransformer() {
        this(SjpEventStoreService.getInstance(),
                SjpViewStoreService.getInstance(),
                ResultingDecisionsConverter.getInstance(),
                ResultingService.getInstance());
    }

    @VisibleForTesting
    DecisionsTransformer(final SjpEventStoreService sjpEventStoreService,
                         final SjpViewStoreService sjpViewStoreService,
                         final ResultingDecisionsConverter resultingDecisionsConverter,
                         final ResultingService resultingService) {
        this.sjpEventStoreService = sjpEventStoreService;
        this.sjpViewStoreService = sjpViewStoreService;
        this.resultingDecisionsConverter = resultingDecisionsConverter;
        this.resultingService = resultingService;
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        final boolean shouldTransform = EVENT_NAME.equals(event.metadata().name())
                || SJP_EVENTS_CASE_ADJOURNED_TO_LATER_SJP_HEARING_RECORDED.equals(event.metadata().name());

        return shouldTransform ? TRANSFORM : NO_ACTION;
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

    private Stream<JsonEnvelope> doApply(final JsonEnvelope currentEvent) {
        final String caseId = currentEvent.asJsonObject().getString(CASE_ID);
        JsonObject decisionPayload;
        Optional<JsonEnvelope> caseStatusChangedEvent = Optional.empty();
        String decisionId;
        if (EVENT_NAME.equals(currentEvent.metadata().name())) {
            decisionPayload = resultingService.getDecisionForACase(UUID.fromString(caseId));
            decisionId = decisionPayload.getString("id");
            caseStatusChangedEvent = buildCaseStatusChangedEvent(currentEvent);
        } else {
            decisionPayload = resultingService.getAdjournPayloadForACase(UUID.fromString(caseId),
                    currentEvent.payloadAsJsonObject().getString("sessionId"),
                    currentEvent.payloadAsJsonObject().getString("adjournedTo"));
            decisionId = decisionPayload.getString("decisionId");
        }

        // check if the decision is already transformed
        if (sjpEventStoreService.decisionTransformed(caseId, decisionId)) {
            return Stream.of(currentEvent);
        }

        LOGGER.info("Transforming case {}", caseId);

        final JsonEnvelope decisionSavedEvent = resultingDecisionsConverter.convert(currentEvent, decisionPayload);

        final Stream.Builder<JsonEnvelope> finalStreamBuilder = Stream.builder();
        finalStreamBuilder.add(decisionSavedEvent);
        finalStreamBuilder.add(currentEvent);

        caseStatusChangedEvent.ifPresent(statusEvent -> finalStreamBuilder.add(statusEvent));

        return finalStreamBuilder.build();
    }

    private Optional<JsonEnvelope> buildCaseStatusChangedEvent(final JsonEnvelope currentEvent) {
        final String caseId = currentEvent.asJsonObject().getString(CASE_ID);
        final JsonObjectBuilder metadataJsonObjectBuilder =
                createObjectBuilderWithFilter(currentEvent.metadata().asJsonObject(),
                        field -> !NAME.equalsIgnoreCase(field));
        metadataJsonObjectBuilder.add(NAME, CaseStatusChanged.EVENT_NAME);
        metadataJsonObjectBuilder.add(ID, UUID.randomUUID().toString());

        final JsonObjectBuilder payloadJsonObjectBuilder = createObjectBuilder();
        sjpViewStoreService.getStatus(caseId)
                .ifPresent((status) -> {
                    if (!"REOPENED_IN_LIBRA".equals(status)) { // we are handling the re-opened in a separate transformation
                        payloadJsonObjectBuilder
                                .add(CASE_ID, currentEvent.asJsonObject().getString(CASE_ID))
                                .add(CASE_STATUS, status);
                    }
                });

        final JsonObject payLoad = payloadJsonObjectBuilder.build();
        if(payLoad.containsKey(CASE_ID)) {
            return Optional.of(envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), payLoad));
        }

        return Optional.empty();
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // not used
    }

}
