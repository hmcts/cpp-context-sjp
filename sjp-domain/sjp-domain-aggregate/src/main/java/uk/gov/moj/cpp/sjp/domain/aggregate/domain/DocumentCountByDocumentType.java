package uk.gov.moj.cpp.sjp.domain.aggregate.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentCountByDocumentType implements Serializable {

    private static final long serialVersionUID = 6085387207075980216L;

    private Map<String, Integer> documentCountByDocumentType = new HashMap<>();

    public void increaseCount(String documentType) {
        if (documentType == null) {
            return;
        }
        documentCountByDocumentType.put(normalise(documentType), getCount(documentType) + 1);
    }

    public Integer getCount(String documentType) {
        if (documentType == null) {
            return 0;
        }
        Integer numberOfDocuments = documentCountByDocumentType.get(normalise(documentType));
        return numberOfDocuments != null ? numberOfDocuments : 0;
    }

    private String normalise(String documentType) {
        return documentType.replaceAll("\\s", "").toLowerCase();
    }
}
