package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.ArrayList;
import java.util.List;

public class TransparencyReportsMetadataView {

    private List<TransparencyReportMetaDataView> reportsMetadata = new ArrayList<>();

    public List<TransparencyReportMetaDataView> getReportsMetadata() {
        return reportsMetadata;
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

            public TransparencyReportMetaDataView build() {
                return transparencyReportMetaDataView;
            }
        }

    }
}
