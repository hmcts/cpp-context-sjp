package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.TransparencyReportMetaDataView;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.transparencyReportMetaDataBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.format;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.resolveSize;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView;

import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;

public class TransparencyReportService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TransparencyReportService.class);

    private static final String ENGLISH = "English";
    private static final String WELSH = "Welsh";

    @Inject
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    public TransparencyReportsMetadataView getMetaData() {
        Optional<TransparencyReportMetadata> latestTransparencyReportMetadata = empty();
        try {
            latestTransparencyReportMetadata = of(transparencyReportMetadataRepository.findLatestTransparencyReportMetadata());
        } catch (NoResultException noResultException) {
            LOGGER.info("No transparency report metadata found:", noResultException);
        }
        final TransparencyReportsMetadataView transparencyReportsMetadataView = new TransparencyReportsMetadataView();

        if (latestTransparencyReportMetadata.isPresent()) {
            // add english report metadata
            transparencyReportsMetadataView.addReportMetaData(buildTransparencyReportMetadata(latestTransparencyReportMetadata.get(), ENGLISH));

            // add welsh report metadata
            transparencyReportsMetadataView.addReportMetaData(buildTransparencyReportMetadata(latestTransparencyReportMetadata.get(), WELSH));
        }

        return transparencyReportsMetadataView;
    }

    private TransparencyReportMetaDataView buildTransparencyReportMetadata(final TransparencyReportMetadata latestTransparencyReportMetadata,
                                                                           final String language) {
        return transparencyReportMetaDataBuilder()
                .withGeneratedAt(format(latestTransparencyReportMetadata.getGeneratedAt()))
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


}
