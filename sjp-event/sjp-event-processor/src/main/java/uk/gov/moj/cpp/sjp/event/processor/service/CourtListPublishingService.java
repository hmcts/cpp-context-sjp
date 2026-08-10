package uk.gov.moj.cpp.sjp.event.processor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.event.processor.helper.HttpConnectionHelper;

import java.io.IOException;

import javax.inject.Inject;

public class CourtListPublishingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListPublishingService.class);
    private static final String PUBLISH_SJP_COURT_LIST_PATH = "/api/court-list-publish/sjp/publishCourtList";

    private HttpConnectionHelper httpConnectionHelper;

    @Inject
    private ApplicationParameters applicationParameters;

    public CourtListPublishingService() {
        this.httpConnectionHelper = new HttpConnectionHelper();
    }

    public CourtListPublishingService(final HttpConnectionHelper httpConnectionHelper, final ApplicationParameters applicationParameters) {
        this.httpConnectionHelper = httpConnectionHelper;
        this.applicationParameters = applicationParameters;
    }

    public Integer publishCourtList(final String payload) throws IOException {
        final String url = applicationParameters.getCourtListPublishingServiceUrl() + PUBLISH_SJP_COURT_LIST_PATH;
        LOGGER.info("publishing court list to url {}, payload size {} bytes", url, payload.length());
        try {
            final Integer responseCode = httpConnectionHelper.getResponseCode(url, payload);
            LOGGER.info("publish court list response from url {}, responseCode {}", url, responseCode);
            return responseCode;
        } catch (final IOException e) {
            LOGGER.error("failed to publish court list to url {}", url, e);
            throw e;
        }
    }
}
