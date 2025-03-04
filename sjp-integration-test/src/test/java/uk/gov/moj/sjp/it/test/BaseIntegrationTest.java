package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.stubNotifications;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypesByJurisdiction;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryVictimSurcharge;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubAllGroupsForUser;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.DELAY_IN_SECONDS;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.INTERVAL_IN_SECONDS;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.util.Defaults;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@ExtendWith(JmsResourceManagementExtension.class)
public abstract class BaseIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseIntegrationTest.class);
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    public static final UUID USER_ID = Defaults.DEFAULT_USER_ID;
    public static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final Map<String, String> cachedStubs = new ConcurrentHashMap<>();

    protected static ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = null;

    static {
        try {
            setDefaultPollDelay(DELAY_IN_SECONDS, SECONDS);
            setDefaultPollInterval(INTERVAL_IN_SECONDS, SECONDS);
            configureFor(HOST, 8080);
            setUpElasticSearch();
        } catch (final Throwable e) {
            LOGGER.error("Failed to set up integration test", e);
        }
    }

    private static void setUpElasticSearch() {
        if(elasticSearchIndexRemoverUtil == null) {
            elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();
            deleteAndCreateIndex();
        }

    }

    protected static void deleteAndCreateIndex() {
        try {
            elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
        } catch (final IOException e) {
            LOGGER.error("Error while creating index ", e);
        }
    }

    @BeforeAll
    public static void setup() {
        LOGGER.info("Setting up integration test stubs once for whole test run");
        resetAllRequests();
        reset();
        stubAllGroupsForUser();
        stubForUserDetails(USER_ID, "ALL");
        stubQueryOffencesByCode(DEFAULT_OFFENCE_CODE);
        stubQueryForVerdictTypesByJurisdiction();
        stubQueryVictimSurcharge();
        stubAllReferenceData();
        stubNotifications();
        stubBailStatuses();
        stubResultIds();
    }
}
