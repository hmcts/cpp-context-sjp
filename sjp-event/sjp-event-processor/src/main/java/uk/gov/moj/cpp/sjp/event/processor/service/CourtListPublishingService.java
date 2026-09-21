package uk.gov.moj.cpp.sjp.event.processor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.event.processor.helper.HttpConnectionHelper;

import java.io.IOException;

import javax.inject.Inject;

public class CourtListPublishingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListPublishingService.class);
    private static final String PUBLISH_SJP_COURT_LIST_PATH = "/api/court-list-publish/sjp/publishCourtList";

    private final HttpConnectionHelper httpConnectionHelper;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    public CourtListPublishingService() {
        this.httpConnectionHelper = new HttpConnectionHelper();
    }

    public void publishCourtList(final String payload) throws IOException {
        final String url = applicationParameters.getCourtListPublishingServiceUrl() + PUBLISH_SJP_COURT_LIST_PATH;
        final String systemUserId = systemIdMapperService.getSystemUserId().toString();
        LOGGER.info("publishing court list to url {}, payload size {} bytes", url, payload.length());
        try {
            final Integer responseCode = httpConnectionHelper.getResponseCode(url, payload, systemUserId);
            LOGGER.info("publish court list response from url {}, responseCode {}", url, responseCode);
        } catch (final IOException e) {
            LOGGER.error("failed to publish court list to url {}", url, e);
            throw e;
        }
    }
}
