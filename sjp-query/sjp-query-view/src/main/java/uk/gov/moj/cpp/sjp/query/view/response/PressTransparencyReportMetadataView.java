package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.format;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.resolveSize;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil;

import java.util.UUID;

public class PressTransparencyReportMetadataView {

    private String generatedAt;

    private int pages;

    private String size;

    private String fileId;

    public PressTransparencyReportMetadataView(final PressTransparencyReportMetadata reportMetadata) {
        this.generatedAt = format(reportMetadata.getGeneratedAt());
        this.pages = ofNullable(reportMetadata.getNumberOfPages()).orElse(0);
        this.size = ofNullable(reportMetadata.getSizeInBytes())
                .map(TransparencyServiceUtil::resolveSize)
                .orElse("0b");
        this.fileId = ofNullable(reportMetadata.getFileServiceId())
                .map(UUID::toString)
                .orElse(null);
    }

    public PressTransparencyReportMetadataView() {
        this.generatedAt = "";
        this.pages = 0;
        this.size = resolveSize(0);
        this.fileId = "0";
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public int getPages() {
        return pages;
    }

    public String getSize() {
        return size;
    }

    public String getFileId() {
        return fileId;
    }
}
