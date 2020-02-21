package uk.gov.moj.cpp.sjp.event.processor.service;

import uk.gov.moj.cpp.sjp.event.processor.helper.HttpConnectionHelper;

import javax.inject.Inject;
import java.io.IOException;

public class AzureFunctionService {

    private static final String HTTPS = "https://";

    private HttpConnectionHelper httpConnectionHelper;

    @Inject
    private ApplicationParameters applicationParameters;

    public AzureFunctionService() {
        this.httpConnectionHelper = new HttpConnectionHelper();
    }

    public AzureFunctionService(final HttpConnectionHelper httpConnectionHelper, final ApplicationParameters applicationParameters) {
        this.httpConnectionHelper = httpConnectionHelper;
        this.applicationParameters = applicationParameters;
    }

    public Integer relayCaseOnCPP(final String payload) throws IOException {
        return httpConnectionHelper.getResponseCode(HTTPS + applicationParameters.getAzureFunctionHostName() + applicationParameters.getRelayCaseOnCppFunctionPath(),payload);
    }
}
