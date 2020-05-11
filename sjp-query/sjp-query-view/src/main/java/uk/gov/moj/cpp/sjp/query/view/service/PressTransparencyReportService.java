package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.PressTransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.PressTransparencyReportMetadataView;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PressTransparencyReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressTransparencyReportService.class);

    @Inject
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    private  static final PressTransparencyReportMetadataView DEFAULT_METADATA = new PressTransparencyReportMetadataView();

    public PressTransparencyReportMetadataView getMetadata(){
        try {
            final PressTransparencyReportMetadata latestPressTransparencyReportMetadata =
                    pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata();
            return ofNullable(latestPressTransparencyReportMetadata)
                    .map(PressTransparencyReportMetadataView::new)
                    .orElse(DEFAULT_METADATA);
        } catch (NoResultException e) {
            LOGGER.info("No press transparency report metadata found:", e);
        }

        return DEFAULT_METADATA;
    }

}
