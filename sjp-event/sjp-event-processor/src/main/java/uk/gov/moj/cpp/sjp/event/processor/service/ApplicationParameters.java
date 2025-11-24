package uk.gov.moj.cpp.sjp.event.processor.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

public class ApplicationParameters {

    @Inject
    @Value(key = "AZURE_FUNCTION_HOST_NAME")
    private String azureFunctionHostName;


    @Inject
    @Value(key = "RELAY_CASE_ON_CPP_FUNCTION_PATH")
    private String relayCaseOnCppFunctionPath;

    
    public String getAzureFunctionHostName() {

        return azureFunctionHostName;
    }

    public String getRelayCaseOnCppFunctionPath() {

        return relayCaseOnCppFunctionPath;
    }
}
