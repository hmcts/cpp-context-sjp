package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEAD_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.UPDATED_DATE;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class PleaUpdatedProcessor {

    public static final String PLEA_CANCELLED_PUBLIC_EVENT_NAME = "public.sjp.plea-cancelled";
    public static final String PLEA_UPDATED_PUBLIC_EVENT_NAME = "public.sjp.plea-updated";

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    protected Sender sender;

    @Inject
    private CaseStateService caseStateService;

    @Inject
    private Clock clock;

    /**
     * @deprecated TODO: REMOVE THE PROCESSOR AND DECOMMISSION THE PleaUpdated event after the event
     * transformation
     */
    @Deprecated
    @Handles(PleaUpdated.EVENT_NAME)
    public void handlePleaUpdated(final JsonEnvelope envelope) {
        final JsonObject pleaUpdatedPayload = envelope.payloadAsJsonObject();
        final PleaType plea = PleaType.valueOf(pleaUpdatedPayload.getString(PLEA));
        handlePleaActions(envelope, plea);
    }

    @Handles(PleadedGuilty.EVENT_NAME)
    public void handlePleadedGuilty(final JsonEnvelope envelope) {
        handlePleaActions(envelope, GUILTY);
    }

    @Handles(PleadedGuiltyCourtHearingRequested.EVENT_NAME)
    public void handlePleadedGuiltyCourtHearingRequested(final JsonEnvelope envelope) {
        handlePleaActions(envelope, GUILTY_REQUEST_HEARING);
    }

    @Handles(PleadedNotGuilty.EVENT_NAME)
    public void handlePleadedNotGuilty(final JsonEnvelope envelope) {
        handlePleaActions(envelope, NOT_GUILTY);
    }

    @Handles(PleaCancelled.EVENT_NAME)
    public void handlePleaCancelled(final JsonEnvelope envelope) {
        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        final UUID offenceId = fromString(envelope.payloadAsJsonObject().getString(OFFENCE_ID));

        raisePleaCancelledPublicEvent(envelope.metadata(), caseId, offenceId);

        // NOTIFYING THE ACTIVITY FOR CASES ALREADY IN SESSION USING THE OLD FLOW
        caseStateService.pleaCancelled(caseId, offenceId, envelope.metadata());
    }

    private void handlePleaActions(final JsonEnvelope envelope, final PleaType pleaType) {
        final JsonObject pleaUpdatedPayload = envelope.payloadAsJsonObject();
        final UUID caseId = fromString(pleaUpdatedPayload.getString(CASE_ID));
        final UUID offenceId = fromString(pleaUpdatedPayload.getString(OFFENCE_ID));
        final ZonedDateTime pleadDate = getPleaUpdatedDate(pleaUpdatedPayload)
                .map(ZonedDateTime::parse)
                .orElseGet(() -> envelope.metadata().createdAt().orElse(clock.now()));

        // NOTIFYING THE ACTIVITY FOR ALREADY STARTING CASES USING THE OLD FLOW
        caseStateService.pleaUpdated(caseId, offenceId, pleaType, pleadDate, envelope.metadata());
    }

    private void raisePleaCancelledPublicEvent(final Metadata metadata, final UUID caseId, final UUID offenceId) {
        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName(PLEA_CANCELLED_PUBLIC_EVENT_NAME)
                .build();

        final JsonObject publicEventPayload = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(OFFENCE_ID, offenceId.toString())
                .build();
        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
    }

    //TODO that's confusing havent' we done event transformation to avoid that?
    private Optional<String> getPleaUpdatedDate(JsonObject pleaPayload) {
        return updatedDate(pleaPayload).isPresent() ? updatedDate(pleaPayload) : pleadDate(pleaPayload);
    }

    private Optional<String> updatedDate(final JsonObject pleaUpdatedPayload) {
        return Optional.ofNullable(pleaUpdatedPayload.getString(UPDATED_DATE, null));
    }

    private Optional<String> pleadDate(final JsonObject pleaUpdatedPayload) {
        return Optional.ofNullable(pleaUpdatedPayload.getString(PLEAD_DATE, null));
    }
}
