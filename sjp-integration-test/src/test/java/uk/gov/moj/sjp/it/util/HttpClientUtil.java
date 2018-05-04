package uk.gov.moj.sjp.it.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.io.File;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtil {
    private static final RestClient restClient = new RestClient();

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final String BASE_URI = System.getProperty("baseUri", "http://localhost:8080");

    private static final String WRITE_BASE_URL = BASE_URI + "/sjp-command-api/command/api/rest/sjp";
    private static final String READ_BASE_URL = BASE_URI + "/sjp-query-api/query/api/rest/sjp";

    public static void makePostCall(String url, String mediaType, String payload) {
        makePostCall(USER_ID, url, mediaType, payload, Response.Status.ACCEPTED);
    }

    public static void makePostCall(String url, String mediaType, String payload, Response.Status expectedStatus) {
        makePostCall(USER_ID, url, mediaType, payload, expectedStatus);
    }

    public static void makePostCall(UUID userId, String url, String mediaType, String payload, Response.Status expectedStatus) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        final String writeUrl = getWriteUrl(url);
        Response response = restClient.postCommand(writeUrl, mediaType, payload, map);
        LOGGER.info("Post call made: \n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tUser = {}\n",
                writeUrl, mediaType, payload, userId);
        assertThat(response.getStatus(), is(expectedStatus.getStatusCode()));
    }

    public static void makeMultipartFormPostCall(UUID userId, String url, String fileFieldName, String fileName) {
        File file = new File(fileName);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, userId.toString());

        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData(fileFieldName, file, MediaType.MULTIPART_FORM_DATA_TYPE, file.getName());
        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
        };
        Response response = ResteasyClientBuilderFactory.clientBuilder().build().target(getWriteUrl(url)).request().headers(headers).post(
                Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE)
        );
        response.close();
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    public static  Response makeGetCall(String url, String mediaType) {
        return makeGetCall(url, mediaType, USER_ID);
    }

    public static Response makeGetCall(String url, String mediaType, UUID userId) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        //FIXME: bug in framework requiring the media type to be added explicitly when using this API call on RestClient
        map.add(HttpHeaders.ACCEPT, mediaType);
        final String readUrl = getReadUrl(url);
        LOGGER.info("Get call made:" + System.lineSeparator()
                        + "Endpoint = {}" + System.lineSeparator()
                        + "Media type = {}" + System.lineSeparator()
                        + "User = {}" + System.lineSeparator(),
                readUrl, mediaType, userId);
        return restClient.query(readUrl, mediaType, map);
    }

    private static String getWriteUrl(String resource) {
        return WRITE_BASE_URL + resource;
    }

    public static String getReadUrl(String resource) {
        return READ_BASE_URL + resource;
    }


}
