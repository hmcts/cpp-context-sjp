package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static uk.gov.moj.sjp.it.stub.AuthorisationServiceStub.stubEnableAllCapabilities;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;

public abstract class BaseIntegrationTest {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    static {
        configureFor(HOST, 8080);
    }


    @Before
    public void setup() {
        WireMock.resetAllRequests();
        stubEnableAllCapabilities();
    }
}
