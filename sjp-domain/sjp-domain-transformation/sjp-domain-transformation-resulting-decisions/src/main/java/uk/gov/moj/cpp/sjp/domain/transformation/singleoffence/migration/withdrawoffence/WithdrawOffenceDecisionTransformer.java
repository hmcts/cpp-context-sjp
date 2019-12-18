package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.withdrawoffence;

import static java.text.MessageFormat.format;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import java.util.Optional;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class WithdrawOffenceDecisionTransformer implements EventTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(WithdrawOffenceDecisionTransformer.class);

    // old events
    static final String ALL_OFFENCES_WITHDRAWAL_REQUESTED = "sjp.events.all-offences-withdrawal-requested";

    static final String ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED = "sjp.events.all-offences-withdrawal-request-cancelled";

    // new events
    private static final String OFFENCE_WITHDRAWAL_REQUESTED = "sjp.events.offence-withdrawal-requested";
    private static final String OFFENCE_WITHDRAWAL_REQUEST_CANCELLED = "sjp.events.offence-withdrawal-request-cancelled";
    private static final String SJP_EVENTS_OFFENCES_WITHDRAWAL_STATUS_SET = "sjp.events.offences-withdrawal-status-set";

    // attributes
    private static final String CANCELLED_BY = "cancelledBy";
    private static final String CANCELLED_AT = "cancelledAt";
    private static final String CASE_ID = "caseId";
    private static final String NAME = "name";
    private static final String OFFENCE_ID = "offenceId";
    private static final String REQUESTED_BY = "requestedBy";
    private static final String REQUESTED_AT = "requestedAt";
    private static final String SET_AT = "setAt";
    private static final String SET_BY = "setBy";
    private static final String WITHDRAWAL_REQUESTS_STATUS = "withdrawalRequestsStatus";
    private static final String WITHDRAWAL_REQUEST_REASON_ID = "withdrawalRequestReasonId";

    private static final String DEFAULT_WITHDRAWAL_REQUEST_REASON_ID = "11b9087a-4681-3484-b2cf-684295353ac6"; // cross check this ??

    private SjpViewStoreService sjpViewStoreService;

    public WithdrawOffenceDecisionTransformer() {
        this(SjpViewStoreService.getInstance());
    }

    @VisibleForTesting
    public WithdrawOffenceDecisionTransformer(final SjpViewStoreService sjpViewStoreService) {
        this.sjpViewStoreService = sjpViewStoreService;
    }


    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        return requires(eventEnvelope.metadata().name()) ? TRANSFORM : NO_ACTION;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // does nothing??
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

    private Stream<JsonEnvelope> doApply(final JsonEnvelope envelope) {

        LOGGER.info("Start of Transform event with Case Id : {}", envelope.payloadAsJsonObject().getString(CASE_ID));

        if (allOffencesWithdrawalRequested(envelope.metadata().name())) {
            return transformAllOffencesWithdrawalRequested(envelope);
        } else if (allOffencesWithdrawalRequestCancelled(envelope.metadata().name())) {
            return transformAllOffencesWithdrawalRequestCancelled(envelope);
        }

        return Stream.of();
    }

    private Stream<JsonEnvelope> transformAllOffencesWithdrawalRequested(final JsonEnvelope envelope) {

        final Stream.Builder<JsonEnvelope> streamBuilder = Stream.builder();

        // get the offence id from the view store
        final Optional<String> offenceIdOptional = sjpViewStoreService.getOffenceId(envelope.payloadAsJsonObject().getString(CASE_ID));
        if (offenceIdOptional.isPresent()) {
            // build offence withdrawal status set event
            final JsonEnvelope offencesWithdrawalStatusSetEnvelope = buildOffenceWithdrawalStatusEnvelope(envelope, offenceIdOptional.get());
            streamBuilder.add(offencesWithdrawalStatusSetEnvelope);

            // build offences withdrawal requested
            final JsonEnvelope offenceWithdrawalRequestedEnvelope = buildOffenceWithdrawalRequestedEnvelope(envelope, offenceIdOptional.get());
            streamBuilder.add(offenceWithdrawalRequestedEnvelope);

            return Stream.of(offencesWithdrawalStatusSetEnvelope, offenceWithdrawalRequestedEnvelope);
        }

        return Stream.of();
    }

    private Stream<JsonEnvelope> transformAllOffencesWithdrawalRequestCancelled(final JsonEnvelope envelope) {

        final Stream.Builder<JsonEnvelope> streamBuilder = Stream.builder();

        // get the offence id from the view store
        final Optional<String> offenceIdOptional = sjpViewStoreService.getOffenceId(envelope.payloadAsJsonObject().getString(CASE_ID));
        if (offenceIdOptional.isPresent()) {

            // build offences withdrawal request cancelled
            final JsonEnvelope offenceWithdrawalRequestedEnvelope = buildOffenceWithdrawalRequestCancelledEnvelope(envelope, offenceIdOptional.get());
            streamBuilder.add(offenceWithdrawalRequestedEnvelope);

            // build offence withdrawal status set event
            final JsonEnvelope offencesWithdrawalStatusSetEnvelope = buildOffenceWithdrawalStatusEnvelope(envelope, offenceIdOptional.get());
            streamBuilder.add(offencesWithdrawalStatusSetEnvelope);


            return Stream.of(offencesWithdrawalStatusSetEnvelope, offenceWithdrawalRequestedEnvelope);
        }

        return Stream.of();
    }

    private JsonEnvelope buildOffenceWithdrawalStatusEnvelope(final JsonEnvelope envelope,
                                                              final String offenceId) {
        final String caseId = envelope.payloadAsJsonObject().getString(CASE_ID);
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(format("userId Null, caseId {0}", caseId)));
        final String createdAt = ZonedDateTimes.toString(envelope.metadata().createdAt().orElseThrow(() -> new IllegalArgumentException(format("createdAt null, caseId {0}", caseId))));

        final JsonObjectBuilder metadataJsonObjectBuilder = createObjectBuilderWithFilter(envelope.metadata().asJsonObject(), field -> !NAME.equalsIgnoreCase(field));
        metadataJsonObjectBuilder.add(NAME, SJP_EVENTS_OFFENCES_WITHDRAWAL_STATUS_SET);

        final JsonObject offenceWithDrawlStatusSetPayload = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(SET_BY, userId)
                .add(SET_AT, createdAt)
                .add(WITHDRAWAL_REQUESTS_STATUS,
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add(OFFENCE_ID, offenceId)
                                        .add(WITHDRAWAL_REQUEST_REASON_ID, DEFAULT_WITHDRAWAL_REQUEST_REASON_ID)
                                        .build())
                                .build())
                .build();

        return envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), offenceWithDrawlStatusSetPayload);
    }

    private JsonEnvelope buildOffenceWithdrawalRequestedEnvelope(final JsonEnvelope envelope,
                                                                 final String offenceId) {
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(format("userId null, offenceId:{0}", offenceId)));
        final String createdAt = ZonedDateTimes.toString(envelope.metadata().createdAt().orElseThrow(() -> new IllegalArgumentException(format("createdAt null, offenceId {0}", offenceId))));
        final String caseId = envelope.payloadAsJsonObject().getString(CASE_ID);

        final JsonObjectBuilder metadataJsonObjectBuilder = createObjectBuilderWithFilter(envelope.metadata().asJsonObject(), field -> !NAME.equalsIgnoreCase(field));
        metadataJsonObjectBuilder.add(NAME, OFFENCE_WITHDRAWAL_REQUESTED);

        final JsonObject offenceWithdrawalRequestedPayload = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(OFFENCE_ID, offenceId)
                .add(WITHDRAWAL_REQUEST_REASON_ID, DEFAULT_WITHDRAWAL_REQUEST_REASON_ID)
                .add(REQUESTED_BY, userId)
                .add(REQUESTED_AT, createdAt)
                .build();

        return envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), offenceWithdrawalRequestedPayload);
    }

    private JsonEnvelope buildOffenceWithdrawalRequestCancelledEnvelope(final JsonEnvelope envelope,
                                                                        final String offenceId) {
        final String createdAt = ZonedDateTimes.toString(envelope.metadata().createdAt().orElseThrow(() -> new IllegalArgumentException(format("createdAt null, offenceId {0}", offenceId))));
        final String caseId = envelope.payloadAsJsonObject().getString(CASE_ID);
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(format("userId null, offenceId:{0}", offenceId)));

        final JsonObjectBuilder metadataJsonObjectBuilder = createObjectBuilderWithFilter(envelope.metadata().asJsonObject(), field -> !NAME.equalsIgnoreCase(field));
        metadataJsonObjectBuilder.add(NAME, OFFENCE_WITHDRAWAL_REQUEST_CANCELLED);

        final JsonObject offenceWithdrawalRequestedPayload = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(OFFENCE_ID, offenceId)
                .add(CANCELLED_BY, userId)
                .add(CANCELLED_AT, createdAt)
                .build();

        return envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), offenceWithdrawalRequestedPayload);
    }

    private boolean requires(String eventName) {
        return allOffencesWithdrawalRequested(eventName)
                || allOffencesWithdrawalRequestCancelled(eventName);
    }

    private boolean allOffencesWithdrawalRequested(String eventName) {
        return ALL_OFFENCES_WITHDRAWAL_REQUESTED.equalsIgnoreCase(eventName);
    }

    private boolean allOffencesWithdrawalRequestCancelled(String eventName) {
        return ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED.equalsIgnoreCase(eventName);
    }
}
