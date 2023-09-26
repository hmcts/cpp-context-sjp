package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypesByJurisdiction;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubAllGroupsForUser;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.DELAY_IN_MILLIS;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.INTERVAL_IN_MILLIS;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.util.Defaults;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseIntegrationTest.class);
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    public static final UUID USER_ID = Defaults.DEFAULT_USER_ID;
    public static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    protected static ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = null;
    protected static ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;

    static {
        Awaitility.setDefaultPollDelay(DELAY_IN_MILLIS, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollInterval(INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
        configureFor(HOST, 8080);
        setUpElasticSearch();
    }

    private static void setUpElasticSearch() {
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        elasticSearchIndexRemoverUtil  = new ElasticSearchIndexRemoverUtil();
        deleteAndCreateIndex();
    }

    protected static void deleteAndCreateIndex() {
        try {
            elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
        }catch (final IOException e){
            LOGGER.error("Error while creating index ", e);
        }
    }

    @BeforeClass
    public static void setup() {
        WireMock.resetAllRequests();
        WireMock.reset();
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubAllGroupsForUser();
        stubForUserDetails(USER_ID, "ALL");
        stubQueryOffencesByCode(DEFAULT_OFFENCE_CODE);
        stubQueryForVerdictTypesByJurisdiction();
    }
}
