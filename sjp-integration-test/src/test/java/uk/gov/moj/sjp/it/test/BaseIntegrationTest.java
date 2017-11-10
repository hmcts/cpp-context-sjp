package uk.gov.moj.sjp.it.test;

import org.junit.Before;

import static uk.gov.moj.sjp.it.stub.AuthorisationServiceStub.stubEnableAllCapabilities;

public abstract class BaseIntegrationTest {

    @Before
    public void setup() {
        stubEnableAllCapabilities();
    }
}
