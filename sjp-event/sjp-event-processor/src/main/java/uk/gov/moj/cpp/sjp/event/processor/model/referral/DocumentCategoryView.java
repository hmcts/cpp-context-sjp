package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class DocumentCategoryView {

    private final DefendantDocumentView defendantDocument;

    public DocumentCategoryView(final DefendantDocumentView defendantDocument) {
        this.defendantDocument = defendantDocument;
    }

    public DefendantDocumentView getDefendantDocument() {
        return defendantDocument;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantDocument);
    }

    @Override
    public String toString() {
        return "DocumentCategoryView{" +
                "defendantDocument=" + defendantDocument +
                '}';
    }
}
