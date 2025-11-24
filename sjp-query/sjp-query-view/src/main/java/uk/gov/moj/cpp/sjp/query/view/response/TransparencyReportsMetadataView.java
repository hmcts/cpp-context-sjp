package uk.gov.moj.cpp.sjp.query.view.response;

import static com.google.common.collect.ImmutableList.copyOf;

import java.util.ArrayList;
import java.util.List;

public class TransparencyReportsMetadataView {

    private List<TransparencyReportMetaDataView> reportsMetadata = new ArrayList<>();

    public List<TransparencyReportMetaDataView> getReportsMetadata() {
        return copyOf(reportsMetadata);
    }

    public void addReportMetaData(final TransparencyReportMetaDataView transparencyReportMetaDataView) {
        reportsMetadata.add(transparencyReportMetaDataView);
    }

    public static TransparencyReportMetaDataView.Builder transparencyReportMetaDataBuilder() {
        return new TransparencyReportMetaDataView.Builder();
    }

    public static class TransparencyReportMetaDataView {
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
            private TransparencyReportMetaDataView transparencyReportMetaDataView = new TransparencyReportMetaDataView();

            public Builder withGeneratedAt(final String generatedAt) {
                transparencyReportMetaDataView.generatedAt = generatedAt;
                return this;
            }

            public Builder withPages(final int pages) {
                transparencyReportMetaDataView.pages = pages;
                return this;
            }

            public Builder withReportIn(final String reportIn) {
                transparencyReportMetaDataView.reportIn = reportIn;
                return this;
            }

            public Builder withSize(final String size) {
                transparencyReportMetaDataView.size = size;
                return this;
            }

            public Builder withFileId(final String fileId) {
                transparencyReportMetaDataView.fileId = fileId;
                return this;
            }

            public Builder withTitle(final String title) {
                transparencyReportMetaDataView.title = title;
                return this;
            }

            public Builder withReportType(final String reportType) {
                transparencyReportMetaDataView.reportType = reportType;
                return this;
            }

            public Builder withLanguage(final String language) {
                transparencyReportMetaDataView.language = language;
                return this;
            }

            public TransparencyReportMetaDataView build() {
                return transparencyReportMetaDataView;
            }
        }

    }
}
