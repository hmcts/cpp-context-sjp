package uk.gov.moj.cpp.sjp.event.processor.utils;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.Test;

public class MetadataHelperTest {

    private MetadataHelper metadataHelper = new MetadataHelper();

    @Test
    public void createsEnvelopeWithProcessIdAndRetrievesItSuccessfully() {
        Metadata metadata = metadataWithRandomUUID("test-name").build();
        JsonObject payload = createObjectBuilder()
                .add("test-key", "test-value")
                .build();

        final JsonEnvelope envelopeWithSjpProcessId = metadataHelper.envelopeWithSjpProcessId(metadata,
                payload,
                "processId");

        final Optional<String> actualProcessId = metadataHelper.getSjpProcessId(envelopeWithSjpProcessId);
        assertTrue(actualProcessId.isPresent());
        assertThat(actualProcessId.get(), is("processId"));
    }

    @Test
    public void returnsEmptyOptionalIfSjpProcessIdNotPresentInMetadata() {
        Metadata metadata = metadataWithRandomUUID("test-name").build();
        JsonObject payload = createObjectBuilder()
                .add("test-key", "test-value")
                .build();

        final JsonEnvelope envelopeWithoutSjpProcessId = envelopeFrom(metadata, payload);
        final Optional<String> actualSjpProcessId = metadataHelper.getSjpProcessId(envelopeWithoutSjpProcessId);
        assertFalse(actualSjpProcessId.isPresent());
    }
}