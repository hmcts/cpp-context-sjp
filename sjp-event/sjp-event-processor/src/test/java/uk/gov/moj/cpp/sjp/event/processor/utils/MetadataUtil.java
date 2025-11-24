package uk.gov.moj.cpp.sjp.event.processor.utils;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MetadataUtil {

    private MetadataUtil() {
    }

    public static Metadata metadataWithNewActionName(final Metadata metadata, final String actionName) {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName(actionName)
                .createdAt(ZonedDateTime.now())
                .withCausation(metadata.causation().toArray(new UUID[metadata.causation().size()]));

        metadata.clientCorrelationId().ifPresent(s -> metadataBuilder.withClientCorrelationId(s));
        metadata.sessionId().ifPresent(s -> metadataBuilder.withSessionId(s));
        metadata.streamId().ifPresent(uuid -> metadataBuilder.withStreamId(uuid));
        metadata.userId().ifPresent(s -> metadataBuilder.withUserId(s));
        metadata.version().ifPresent(aLong -> metadataBuilder.withVersion(aLong));

        return metadataBuilder.build();
    }
}