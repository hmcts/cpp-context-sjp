package uk.gov.moj.cpp.sjp.event.processor.service;

import uk.gov.moj.cpp.sjp.event.processor.helper.HttpConnectionHelper;

import java.io.IOException;

import javax.inject.Inject;

public class CourtListPublishingService {

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
        return httpConnectionHelper.getResponseCode(
                applicationParameters.getCourtListPublishingServiceUrl() + PUBLISH_SJP_COURT_LIST_PATH, payload);
    }
}
