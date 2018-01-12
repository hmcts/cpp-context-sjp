package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubAllGroupsForUser;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.io.File;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractTestHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestHelper.class);

    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "public.event";
    public static final String USER_ID = UUID.randomUUID().toString();
    private static final String STRUCTURE_SYSTEM_USER = "38e4b0c2-b4d4-4078-a857-7a5570e7ae73";

    protected static final String BASE_URI = System.getProperty("baseUri", "http://localhost:8080");
    protected static final String SJP_EVENT_TOPIC = "sjp.event";
    private static final String WRITE_BASE_URL = "/sjp-command-api/command/api/rest/sjp";
    private static final String READ_BASE_URL = "/sjp-query-api/query/api/rest/sjp";

    protected final RestClient restClient = new RestClient();

    protected MessageConsumerClient publicConsumer = new MessageConsumerClient();
    protected MessageConsumer privateEventsConsumer;
    protected MessageConsumer publicEventsConsumer;

    public static String getWriteUrl(String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    public static String getReadUrl(String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    static {
        doAllStubbing();
    }

    public static void doAllStubbing() {
        InternalEndpointMockUtils.stubPingFor("usersgroups-query-api");
        stubAllGroupsForUser(USER_ID);
        stubAllGroupsForUser(STRUCTURE_SYSTEM_USER);
    }

    protected void makePostCall(String url, String mediaType, String payload) {
        makePostCall(UUID.fromString(USER_ID), url, mediaType, payload);
    }

    protected void makePostCall(UUID userId, String url, String mediaType, String payload) {
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", url, mediaType, payload, USER_ID);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        Response response = restClient.postCommand(url, mediaType, payload, map);
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    protected void makeMultipartFormPostCall(UUID userId, String url, String fileFieldName, String fileName) {
        File file = new File(fileName);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, userId.toString());

        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData(fileFieldName, file, MediaType.MULTIPART_FORM_DATA_TYPE, file.getName());
        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
        };
        Response response = ResteasyClientBuilderFactory.clientBuilder().build().target(url).request().headers(headers).post(
                Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE)
        );
        response.close();
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    protected Response makeGetCall(String url, String mediaType) {
        return makeGetCall(url, mediaType, USER_ID);
    }

    protected Response makeGetCall(String url, String mediaType, String userId) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        //FIXME: bug in framework requiring the media type to be added explicitly when using this API call on RestClient
        map.add(HttpHeaders.ACCEPT, mediaType);
        Response response = restClient.query(url, mediaType, map);
        LOGGER.info("Get call made: \n\n\tEndpoint = {} \n\tMedia type = {}\n\n", url, mediaType, userId);
        return response;
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
