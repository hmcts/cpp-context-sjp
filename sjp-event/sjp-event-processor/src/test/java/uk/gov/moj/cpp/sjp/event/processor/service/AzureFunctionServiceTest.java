package uk.gov.moj.cpp.sjp.event.processor.service;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.event.processor.helper.HttpConnectionHelper;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AzureFunctionServiceTest {
    private final String PAYLOAD = "dummy payload";
    @Mock
    HttpConnectionHelper httpConnectionHelper;

    @Mock
    private ApplicationParameters applicationParameters;

    @BeforeEach
    public void initMocks() {
        Mockito.when(applicationParameters.getAzureFunctionHostName()).thenReturn("hostname");
        Mockito.when(applicationParameters.getRelayCaseOnCppFunctionPath()).thenReturn("RelayCaseOnCppFunctionPath");
    }

    @Test
    public void relayCaseOnCPP() throws IOException {
        Mockito.when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenReturn(HttpStatus.SC_ACCEPTED);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters);
        Integer response =azureFunctionService.relayCaseOnCPP(PAYLOAD);
    }

    @Test
    public void relayCaseOnCPPFailed() throws IOException {

        Mockito.when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenThrow(IOException.class);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters);
        assertThrows(IOException.class, () -> azureFunctionService.relayCaseOnCPP(PAYLOAD));
    }
}