package uk.gov.moj.cpp.sjp.event.processor.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.messaging.Envelope;

import java.util.UUID;

public class SystemDocGeneratorEnvelopeMatchers {

    public static Envelope<?> endorsementRemovalNotificationGeneratedCommand(final UUID applicationDecisionId,
                                                                             final UUID documentFileServiceId) {
        return argThat(jsonEnvelope(
                metadata().withName("sjp.command.endorsement-removal-notification-generated"),
                payload().isJson(allOf(
                        withJsonPath("$.applicationDecisionId", equalTo(applicationDecisionId.toString())),
                        withJsonPath("$.fileId", equalTo(documentFileServiceId.toString()))
                ))));
    }

    public static Envelope<?> endorsementRemovalNotificationGenerationFailedCommand(final UUID applicationDecisionId) {
        return argThat(jsonEnvelope(
                metadata().withName("sjp.command.endorsement-removal-notification-generation-failed"),
                payload().isJson(allOf(
                        withJsonPath("$.applicationDecisionId", equalTo(applicationDecisionId.toString()))
                ))));
    }
}
