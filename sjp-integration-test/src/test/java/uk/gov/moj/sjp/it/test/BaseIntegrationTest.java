package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubAllGroupsForUser;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.DELAY_IN_MILLIS;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.INTERVAL_IN_MILLIS;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.util.Defaults;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.awaitility.Awaitility;
import org.junit.BeforeClass;

public abstract class BaseIntegrationTest {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    public static final UUID USER_ID = Defaults.DEFAULT_USER_ID;
    protected final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();

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
        stubQueryOffencesByCode(DEFAULT_OFFENCE_CODE);
    }

    protected void cleanDb() {
        viewStoreCleaner.cleanEventStoreTables();
        viewStoreCleaner.cleanViewstoreTables();
    }
}
