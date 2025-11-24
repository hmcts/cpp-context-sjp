package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DocumentTypeAccess {
    private final UUID documentTypeId;
    private final String documentType;

    public DocumentTypeAccess(final UUID documentTypeId, final String documentType) {
        this.documentTypeId = documentTypeId;
        this.documentType = documentType;
    }

    public UUID getDocumentTypeId() {
        return documentTypeId;
    }

    public String getDocumentType() {
        return documentType;
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
