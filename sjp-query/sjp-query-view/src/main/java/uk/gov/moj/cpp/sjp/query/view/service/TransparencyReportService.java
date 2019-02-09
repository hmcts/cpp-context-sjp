package uk.gov.moj.cpp.sjp.query.view.service;

import static java.time.format.DateTimeFormatter.ofPattern;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.TransparencyReportMetaDataView;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.transparencyReportMetaDataBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.resolveSize;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView;

import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

public class TransparencyReportService {

    private static final String DATE_TIME_PATTERN = "d MMMM YYYY 'at' K:mma";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = ofPattern(DATE_TIME_PATTERN);
    private static final String ENGLISH = "English";
    private static final String WELSH = "Welsh";

    @Inject
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    public TransparencyReportsMetadataView getMetaData() {

        final TransparencyReportMetadata latestTransparencyReportMetadata = transparencyReportMetadataRepository.findLatestTransparencyReportMetadata();
        final TransparencyReportsMetadataView transparencyReportsMetadataView = new TransparencyReportsMetadataView();

        // add english report metadata
        transparencyReportsMetadataView.addReportMetaData(buildTransparencyReportMetadata(latestTransparencyReportMetadata, ENGLISH));

        // add welsh report metadata
        transparencyReportsMetadataView.addReportMetaData(buildTransparencyReportMetadata(latestTransparencyReportMetadata, WELSH));


        return transparencyReportsMetadataView;
    }

    private TransparencyReportMetaDataView buildTransparencyReportMetadata(final TransparencyReportMetadata latestTransparencyReportMetadata,
                                                                           final String language) {
        return transparencyReportMetaDataBuilder()
                .withGeneratedAt(reformat(latestTransparencyReportMetadata))
                .withReportIn(language)
                .withPages(WELSH.equals(language) ?
                        latestTransparencyReportMetadata.getWelshNumberOfPages()
                        : latestTransparencyReportMetadata.getEnglishNumberOfPages())
                .withSize(resolveSize(WELSH.equals(language) ?
                        latestTransparencyReportMetadata.getWelshSizeInBytes()
                        : latestTransparencyReportMetadata.getEnglishSizeInBytes()))
                .withFileId(WELSH.equals(language) ?
                        latestTransparencyReportMetadata.getWelshFileServiceId().toString()
                        : latestTransparencyReportMetadata.getEnglishFileServiceId().toString()).build();
    }

    private String reformat(final TransparencyReportMetadata latestTransparencyReportMetadata) {
        return latestTransparencyReportMetadata
                .getGeneratedAt()
                .format(DATE_TIME_FORMATTER)
                .replace("AM", "am")
                .replace("PM", "pm");
    }

}
