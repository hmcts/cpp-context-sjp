package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready;


import java.time.LocalDate;

public class CaseMarkedReadyReadyMetaData {

    private final String metaDataId;

    private final LocalDate markedAt;

    public CaseMarkedReadyReadyMetaData(final String metaDataId,
                                        final LocalDate markedAt) {
        this.metaDataId = metaDataId;
        this.markedAt = markedAt;
    }

    public String getMetaDataId() {
        return metaDataId;
    }

    public LocalDate getMarkedAt() {
        return markedAt;
    }

}
