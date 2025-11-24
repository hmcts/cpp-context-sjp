package uk.gov.moj.cpp.sjp.query.view.service;

import static java.time.LocalDateTime.now;
import static uk.gov.moj.cpp.sjp.query.view.response.PressTransparencyReportMetadataView.pressTransparencyReportMetaDataBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.format;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.PressTransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.PressTransparencyReportMetadataView;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PressTransparencyReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressTransparencyReportService.class);

    @Inject
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    public PressTransparencyReportMetadataView getMetadata() {
        final PressTransparencyReportMetadataView pressTransparencyReportsMetadataView = new PressTransparencyReportMetadataView();
        try {
            final List<PressTransparencyReportMetadata> reportMetadataList = pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata(now().minusDays(1));
            reportMetadataList.forEach(metadata -> pressTransparencyReportsMetadataView.addReportMetaData(buildPressTransparencyReportMetadata(metadata)));
            return pressTransparencyReportsMetadataView;
        } catch (NoResultException e) {
            LOGGER.info("No press transparency report metadata found:", e);
        }
        return pressTransparencyReportsMetadataView;
    }


    private PressTransparencyReportMetadataView.PressTransparencyReportMetaDataView buildPressTransparencyReportMetadata(final PressTransparencyReportMetadata latestPressTransparencyReportMetadata) {

        final String language = latestPressTransparencyReportMetadata.getLanguage();
        return pressTransparencyReportMetaDataBuilder()
                .withGeneratedAt(format(latestPressTransparencyReportMetadata.getGeneratedAt()))
                .withReportIn(language.charAt(0) + language.substring(1).toLowerCase())
                .withPages(latestPressTransparencyReportMetadata.getNumberOfPages())
                .withSize(latestPressTransparencyReportMetadata.getSizeInBytes().toString())
                .withFileId(latestPressTransparencyReportMetadata.getFileServiceId().toString())
                .withTitle(latestPressTransparencyReportMetadata.getTitle()).build();
    }

}
