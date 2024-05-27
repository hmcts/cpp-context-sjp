package uk.gov.moj.cpp.sjp.query.view.service;

import static java.time.LocalDateTime.now;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.TransparencyReportMetaDataView;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.transparencyReportMetaDataBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.format;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;

public class TransparencyReportService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TransparencyReportService.class);

    @Inject
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    public TransparencyReportsMetadataView getMetaData() {
        final TransparencyReportsMetadataView transparencyReportsMetadataView = new TransparencyReportsMetadataView();
        try {
            final List<TransparencyReportMetadata> reportMetadataList = transparencyReportMetadataRepository.findLatestTransparencyReportMetadata(now().minusDays(1));
            reportMetadataList.forEach(metadata -> transparencyReportsMetadataView.addReportMetaData(buildTransparencyReportMetadata(metadata)));
            return transparencyReportsMetadataView;
        } catch (NoResultException e) {
            LOGGER.info("No transparency report metadata found:", e);
        }
        return transparencyReportsMetadataView;
    }

    private TransparencyReportMetaDataView buildTransparencyReportMetadata(final TransparencyReportMetadata latestTransparencyReportMetadata) {

        final String language = latestTransparencyReportMetadata.getLanguage();
        return transparencyReportMetaDataBuilder()
                .withGeneratedAt(format(latestTransparencyReportMetadata.getGeneratedAt()))
                .withReportIn(language.charAt(0) + language.substring(1).toLowerCase())
                .withPages(latestTransparencyReportMetadata.getNumberOfPages())
                .withSize(latestTransparencyReportMetadata.getSizeInBytes().toString())
                .withFileId(latestTransparencyReportMetadata.getFileServiceId().toString())
                .withTitle(latestTransparencyReportMetadata.getTitle()).build();
    }
}
