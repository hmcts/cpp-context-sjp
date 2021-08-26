package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DocumentCategoryView {

    private final ApplicationDocumentView applicationDocument;
    private final DefendantDocumentView defendantDocument;

    public DocumentCategoryView(final DefendantDocumentView defendantDocument) {
        this.defendantDocument = defendantDocument;
        this.applicationDocument = null;
    }

    public DocumentCategoryView(final ApplicationDocumentView applicationDocument) {
        this.applicationDocument = applicationDocument;
        this.defendantDocument = null;
    }

    public DefendantDocumentView getDefendantDocument() {
        return defendantDocument;
    }

    public ApplicationDocumentView getApplicationDocument() {
        return applicationDocument;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantDocument, applicationDocument);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("applicationDocument", applicationDocument)
                .append("defendantDocument", defendantDocument)
                .toString();
    }
}
