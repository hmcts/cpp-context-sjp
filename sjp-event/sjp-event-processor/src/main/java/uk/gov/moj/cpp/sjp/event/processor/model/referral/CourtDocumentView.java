package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class CourtDocumentView {

    private final UUID courtDocumentId;
    private final DocumentCategoryView documentCategory;
    private final String name;
    private final UUID documentTypeId;
    private final String mimeType;

    private final List<MaterialView> materials;

    public CourtDocumentView(final UUID courtDocumentId,
                             final DocumentCategoryView documentCategory,
                             final String name,
                             final UUID documentTypeId,
                             final String mimeType,
                             final List<MaterialView> materials) {
        this.courtDocumentId = courtDocumentId;
        this.documentCategory = documentCategory;
        this.name = name;
        this.documentTypeId = documentTypeId;
        this.mimeType = mimeType;
        this.materials = materials;
    }

    public UUID getCourtDocumentId() {
        return courtDocumentId;
    }

    public DocumentCategoryView getDocumentCategory() {
        return documentCategory;
    }

    public String getName() {
        return name;
    }

    public UUID getDocumentTypeId() {
        return documentTypeId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public List<MaterialView> getMaterials() {
        return materials;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courtDocumentId, documentCategory, name, documentTypeId, mimeType, materials);
    }

    @Override
    public String toString() {
        return "CourtDocumentView{" +
                "courtDocumentId=" + courtDocumentId +
                ", documentCategory=" + documentCategory +
                ", name='" + name + '\'' +
                ", documentTypeId=" + documentTypeId +
                ", mimeType='" + mimeType + '\'' +
                ", materials=" + materials +
                '}';
    }
}
