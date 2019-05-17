package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubAllGroupsForUser;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.DELAY_IN_MILLIS;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.INTERVAL_IN_MILLIS;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.awaitility.Awaitility;
import org.junit.BeforeClass;

public abstract class BaseIntegrationTest {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    public static final UUID USER_ID = UUID.fromString("58ea6e5f-193d-49cc-af43-edfed4f5e5fc");

    static {
        Awaitility.setDefaultPollDelay(DELAY_IN_MILLIS, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollInterval(INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
        configureFor(HOST, 8080);
    }

    @BeforeClass
    public static void setup() {
        WireMock.resetAllRequests();
        WireMock.reset();
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubAllGroupsForUser();
        stubForUserDetails(USER_ID, "ALL");
    }
}
