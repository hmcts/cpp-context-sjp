package uk.gov.moj.cpp.sjp.event.processor.utils.builders;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObjectBuilder;

public class DocumentAvailableEventEnvelopeBuilder {

    private static final String EVENT_NAME = "public.systemdocgenerator.events.document-available";

    private String conversionFormat;
    private UUID documentFileServiceId;
    private Integer generateVersion;
    private ZonedDateTime generatedTime;
    private String originatingSource;
    private UUID payloadFileServiceId;
    private ZonedDateTime requestedTime;
    private UUID sourceCorrelationId;
    private String templateIdentifier;

    private DocumentAvailableEventEnvelopeBuilder(final String conversionFormat,
                                                  final UUID documentFileServiceId,
                                                  final Integer generateVersion,
                                                  final ZonedDateTime generatedTime,
                                                  final String originatingSource,
                                                  final UUID payloadFileServiceId,
                                                  final ZonedDateTime requestedTime
    ) {
        this.conversionFormat = conversionFormat;
        this.documentFileServiceId = documentFileServiceId;
        this.generateVersion = generateVersion;
        this.generatedTime = generatedTime;
        this.originatingSource = originatingSource;
        this.payloadFileServiceId = payloadFileServiceId;
        this.requestedTime = requestedTime;
    }

    public static DocumentAvailableEventEnvelopeBuilder withDefaults() {
        final String conversionFormat = "pdf";
        final UUID documentFileServiceId = randomUUID();
        final int generateVersion = 1;
        final ZonedDateTime generatedTime = ZonedDateTime.now();
        final String originatingSource = "sjp";
        final UUID payloadFileServiceId = randomUUID();
        final ZonedDateTime requestedTime = ZonedDateTime.now();
        return new DocumentAvailableEventEnvelopeBuilder(conversionFormat,
                documentFileServiceId,
                generateVersion,
                generatedTime,
                originatingSource,
                payloadFileServiceId,
                requestedTime);
    }

    public DocumentAvailableEventEnvelopeBuilder originatingSource(final String originatingSource) {
        this.originatingSource = originatingSource;
        return this;
    }

    public DocumentAvailableEventEnvelopeBuilder templateIdentifier(final String templateIdentifier) {
        this.templateIdentifier = templateIdentifier;
        return this;
    }

    public DocumentAvailableEventEnvelopeBuilder sourceCorrelationId(final UUID sourceCorrelationId) {
        this.sourceCorrelationId = sourceCorrelationId;
        return this;
    }

    public JsonEnvelope envelope() {
        final JsonObjectBuilder payload = createObjectBuilder().add("payloadFileServiceId", randomUUID().toString())
                .add("conversionFormat", this.conversionFormat)
                .add("documentFileServiceId", this.documentFileServiceId.toString())
                .add("generateVersion", this.generateVersion.toString())
                .add("generatedTime", this.generatedTime.toString())
                .add("originatingSource", this.originatingSource)
                .add("payloadFileServiceId", this.payloadFileServiceId.toString())
                .add("requestedTime", this.requestedTime.toString());

        addOptional("sourceCorrelationId", this.sourceCorrelationId, payload);
        addOptional("templateIdentifier", templateIdentifier, payload);

        return EnvelopeFactory.createEnvelope(EVENT_NAME, payload.build());
    }

    public String getConversionFormat() {
        return conversionFormat;
    }

    public UUID getDocumentFileServiceId() {
        return documentFileServiceId;
    }

    public Integer getGenerateVersion() {
        return generateVersion;
    }

    public ZonedDateTime getGeneratedTime() {
        return generatedTime;
    }

    public String getOriginatingSource() {
        return originatingSource;
    }

    public UUID getPayloadFileServiceId() {
        return payloadFileServiceId;
    }

    public ZonedDateTime getRequestedTime() {
        return requestedTime;
    }

    public UUID getSourceCorrelationId() {
        return sourceCorrelationId;
    }

    public String getTemplateIdentifier() {
        return templateIdentifier;
    }

    private void addOptional(final String key, final String value, final JsonObjectBuilder payload) {
        if (isNull(value)) {
            payload.addNull(key);
        } else {
            payload.add(key, value);
        }
    }

    private void addOptional(final String key, final UUID value, final JsonObjectBuilder payload) {
        if (isNull(value)) {
            addOptional(key, (String) null, payload);
        } else {
            addOptional(key, value.toString(), payload);
        }
    }
}
