package uk.gov.moj.cpp.sjp.event.processor.utils.builders;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.json.JsonObjectBuilder;

public class GenerationFailedEventEnvelopeBuilder {

    private static final String EVENT_NAME = "public.systemdocgenerator.events.generation-failed";
    private String sourceCorrelationId;
    private String templateIdentifier;
    private JsonObjectBuilder payload;

    public GenerationFailedEventEnvelopeBuilder() {
        this.sourceCorrelationId = randomUUID().toString();
        this.templateIdentifier = "PendingCasesEnglish";
        this.payload = createObjectBuilder();
    }

    public GenerationFailedEventEnvelopeBuilder templateIdentifier(final String templateIdentifier) {
        this.templateIdentifier = templateIdentifier;
        return this;
    }

    public GenerationFailedEventEnvelopeBuilder sourceCorrelationId(final String sourceCorrelationId) {
        this.sourceCorrelationId = sourceCorrelationId;
        return this;
    }

    public String getSourceCorrelationId() {
        return this.sourceCorrelationId;
    }

    public JsonEnvelope envelope() {
        payload.add("payloadFileServiceId", randomUUID().toString())
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", sourceCorrelationId)
                .add("originatingSource", "sjp")
                .add("reason", "Could not process the request");
        addOptional("templateIdentifier", templateIdentifier);

        return EnvelopeFactory.createEnvelope(EVENT_NAME, payload.build());
    }

    private void addOptional(final String key, final String value) {
        if (isNull(value)) {
            payload.addNull(key);
        } else {
            payload.add(key, value);
        }
    }
}
