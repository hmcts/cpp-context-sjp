package uk.gov.moj.cpp.sjp.event.processor.utils;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.justice.services.messaging.JsonEnvelope.METADATA;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetadataHelperTest {
    private JsonObject payload;
    private Metadata metadata;
    private MetadataHelper metadataHelper;

    @BeforeEach
    public void setup() {
        metadata = metadataWithRandomUUID("test-name").build();
        payload = createObjectBuilder()
                .add("test-key", "test-value")
                .build();
        metadataHelper = new MetadataHelper();
    }

    @Test
    public void shouldCreateEnvelopeWithCustomMetadataAndRetrievesItSuccessfully() {
        final JsonObject customMetadata = createObjectBuilder().add("key1", "value1").build();

        final JsonEnvelope envelopeWithCustomMetadata = metadataHelper.envelopeWithCustomMetadata(metadata, customMetadata, payload);

        final JsonObject expectedPayload = createObjectBuilder()
                .add("test-key", "test-value")
                .add(METADATA, envelopeWithCustomMetadata.metadata().asJsonObject())
                .build();

        final Optional<JsonObject> actualCustomMetadata = metadataHelper.getSjpMetadata(envelopeWithCustomMetadata);

        assertThat(actualCustomMetadata, is(Optional.of(customMetadata)));
        assertThat(envelopeWithCustomMetadata.payloadAsJsonObject(), is(expectedPayload));
    }

    @Test
    public void shouldCreateEnvelopeWithSjpIdAndRetrievesItSuccessfully() {
        final String sjpId = randomAlphanumeric(10);

        final JsonEnvelope envelopeWithSjpProcessId = metadataHelper.enrichMetadataWithProcessId(metadata, payload, sjpId);

        final JsonObject expectedPayload = createObjectBuilder()
                .add("test-key", "test-value")
                .add(METADATA, envelopeWithSjpProcessId.metadata().asJsonObject())
                .build();

        assertThat(envelopeWithSjpProcessId.payloadAsJsonObject(), is(expectedPayload));
        assertThat(metadataHelper.getSjpProcessId(envelopeWithSjpProcessId), is(Optional.of(sjpId)));
    }

    @Test
    public void returnsEmptyOptionalIfSjpProcessIdNotPresentInMetadata() {
        final JsonEnvelope envelopeWithoutSjpProcessId = envelopeFrom(metadata, payload);

        assertThat(metadataHelper.getSjpProcessId(envelopeWithoutSjpProcessId), is(Optional.empty()));
    }

    @Test
    public void returnsEmptyOptionalIfCustomMetadataNotPresentInMetadata() {
        final JsonEnvelope envelopeWithoutSjpProcessId = envelopeFrom(metadata, payload);

        final Optional<JsonObject> actualCustomMetadata = metadataHelper.getSjpMetadata(envelopeWithoutSjpProcessId);

        assertFalse(actualCustomMetadata.isPresent());
    }
}