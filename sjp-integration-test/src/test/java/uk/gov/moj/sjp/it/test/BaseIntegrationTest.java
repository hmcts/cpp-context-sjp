package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static uk.gov.moj.sjp.it.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubAllGroupsForUser;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.BeforeClass;

public abstract class BaseIntegrationTest {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    public static final UUID USER_ID = UUID.fromString("58ea6e5f-193d-49cc-af43-edfed4f5e5fc");
    private static final UUID SJP_SYSTEM_USER = UUID.fromString("38e4b0c2-b4d4-4078-a857-7a5570e7ae73");

    static {
        configureFor(HOST, 8080);
    }

    @BeforeClass
    public static void setup() {
        WireMock.resetAllRequests();
        stubEnableAllCapabilities();
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubAllGroupsForUser(USER_ID);
        stubAllGroupsForUser(SJP_SYSTEM_USER);
        stubForUserDetails(USER_ID, "ALL");
    }
}
