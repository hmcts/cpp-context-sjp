package uk.gov.moj.cpp.sjp.query.view.response;

import static com.google.common.collect.ImmutableList.copyOf;

import java.util.ArrayList;
import java.util.List;

public class PressTransparencyReportMetadataView {

    private final List<PressTransparencyReportMetaDataView> reportsMetadata = new ArrayList<>();

    public static PressTransparencyReportMetaDataView.Builder pressTransparencyReportMetaDataBuilder() {
        return new PressTransparencyReportMetaDataView.Builder();
    }

    public List<PressTransparencyReportMetaDataView> getReportsMetadata() {
        return copyOf(reportsMetadata);
    }

    public void addReportMetaData(final PressTransparencyReportMetaDataView pressTransparencyReportMetaDataView) {
        reportsMetadata.add(pressTransparencyReportMetaDataView);
    }

    public static class PressTransparencyReportMetaDataView {
        private String generatedAt;
        private int pages;
        private String reportIn;
        private String size;
        private String fileId;
        private String title;
        private String reportType;
        private String language;

        public String getGeneratedAt() {
            return generatedAt;
        }

        public int getPages() {
            return pages;
        }

        public String getReportIn() {
            return reportIn;
        }

        public String getSize() {
            return size;
        }

        public String getFileId() {
            return fileId;
        }

        public String getTitle() {
            return title;
        }

        public String getReportType() {
            return reportType;
        }

        public String getLanguage() {
            return language;
        }

        public static class Builder {
            private final PressTransparencyReportMetaDataView transparencyReportMetaDataView = new PressTransparencyReportMetaDataView();

            public PressTransparencyReportMetaDataView.Builder withGeneratedAt(final String generatedAt) {
                transparencyReportMetaDataView.generatedAt = generatedAt;
                return this;
            }

            public PressTransparencyReportMetaDataView.Builder withPages(final int pages) {
                transparencyReportMetaDataView.pages = pages;
                return this;
            }

            public PressTransparencyReportMetaDataView.Builder withReportIn(final String reportIn) {
                transparencyReportMetaDataView.reportIn = reportIn;
                return this;
            }

            public PressTransparencyReportMetaDataView.Builder withSize(final String size) {
                transparencyReportMetaDataView.size = size;
                return this;
            }

            public PressTransparencyReportMetaDataView.Builder withFileId(final String fileId) {
                transparencyReportMetaDataView.fileId = fileId;
                return this;
            }

            public PressTransparencyReportMetaDataView.Builder withTitle(final String title) {
                transparencyReportMetaDataView.title = title;
                return this;
            }

            public PressTransparencyReportMetaDataView.Builder withReportType(final String reportType) {
                transparencyReportMetaDataView.reportType = reportType;
                return this;
            }

            public PressTransparencyReportMetaDataView.Builder withLanguage(final String language) {
                transparencyReportMetaDataView.language = language;
                return this;
            }

            public PressTransparencyReportMetaDataView build() {
                return transparencyReportMetaDataView;
            }
        }

    }
}
