package uk.gov.moj.cpp.sjp.domain.transformation;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpEventStoreService;

import java.util.stream.Stream;

import org.slf4j.Logger;

@Transformation
public class DeactivateUnlinkedDefendantUpdateFailed implements EventTransformation {

    private static final String EVENTS_DEFENDANT_UPDATE_FAILED = "sjp.events.defendant-update-failed";
    private static final long EVENT_FIRST_IN_STREAM_POSITION = 1L;
    private static final Logger LOGGER = getLogger(DeactivateUnlinkedDefendantUpdateFailed.class);

    private SjpEventStoreService sjpEventStoreService;

    public DeactivateUnlinkedDefendantUpdateFailed() {
        this.sjpEventStoreService = SjpEventStoreService.getInstance();
    }

    @Override
    @SuppressWarnings("squid:S2629")
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        final Metadata metadata = eventEnvelope.metadata();
        LOGGER.info("checking {} {} for {}", metadata.name(), metadata.position().orElse(-1l), metadata.streamId().orElse(null));
        if(metadata.name().equals(EVENTS_DEFENDANT_UPDATE_FAILED)
                && metadata.position().orElse(0L) != EVENT_FIRST_IN_STREAM_POSITION) {
            return metadata.streamId()
                    .map(streamId -> sjpEventStoreService.hasInitialEventInStream(streamId.toString()))
                    .map(initialEventPresent -> initialEventPresent ? NO_ACTION : DEACTIVATE)
                    .orElse(NO_ACTION);
        }
        return NO_ACTION;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        //Not used
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        return Stream.empty();
    }
}
