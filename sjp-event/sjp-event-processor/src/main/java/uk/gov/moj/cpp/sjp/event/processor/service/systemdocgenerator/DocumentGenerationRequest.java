package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DocumentGenerationRequest {

    private static final String ORIGINATING_SOURCE = "sjp";

    private TemplateIdentifier templateIdentifier;
    private ConversionFormat conversionFormat;
    private String sourceCorrelationId;
    private UUID payloadFileServiceId;

    public DocumentGenerationRequest(final TemplateIdentifier templateIdentifier,
                                     final ConversionFormat conversionFormat,
                                     final String sourceCorrelationId,
                                     final UUID payloadFileServiceId) {
        this.templateIdentifier = templateIdentifier;
        this.conversionFormat = conversionFormat;
        this.sourceCorrelationId = sourceCorrelationId;
        this.payloadFileServiceId = payloadFileServiceId;
    }

    public String getOriginatingSource() {
        return ORIGINATING_SOURCE;
    }

    public TemplateIdentifier getTemplateIdentifier() {
        return templateIdentifier;
    }

    public ConversionFormat getConversionFormat() {
        return conversionFormat;
    }

    public String getSourceCorrelationId() {
        return sourceCorrelationId;
    }

    public UUID getPayloadFileServiceId() {
        return payloadFileServiceId;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
