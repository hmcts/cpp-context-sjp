package uk.gov.moj.sjp.it.util;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
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


    private static final String BASE_URI = getBaseUri();

    private static final String WRITE_BASE_URL = BASE_URI + "/sjp-command-api/command/api/rest/sjp";
    private static final String READ_BASE_URL = BASE_URI + "/sjp-query-api/query/api/rest/sjp";
    public static final ResteasyClient RESTEASY_CLIENT = ResteasyClientBuilderFactory.clientBuilder().build();

    public static UUID makePostCall(final String url, final String mediaType, final String payload) {
        return makePostCall(USER_ID, url, mediaType, payload, Response.Status.ACCEPTED);
    }

    public static UUID makePostCall(final String url, final String mediaType, final String payload, final Response.Status expectedStatus) {
        return makePostCall(USER_ID, url, mediaType, payload, expectedStatus);
    }

    public static UUID makePostCall(final UUID userId, final String url, final String mediaType, final String payload, final Response.Status expectedStatus) {
        final UUID correlationId = randomUUID();

        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        map.add(HeaderConstants.CLIENT_CORRELATION_ID, correlationId);

        final String writeUrl = getWriteUrl(url);
        try (Response response = restClient.postCommand(writeUrl, mediaType, payload, map)) {
            String responseBody = "";
            try {
                responseBody = response.readEntity(String.class);
            } catch (IllegalStateException e) {
                //no-op in case of no response
            }
            assertThat(format("Post returned not expected status code with body: %s", responseBody),
                    response.getStatus(), is(expectedStatus.getStatusCode()));
        }

        return correlationId;
    }

    public static String getPostCallResponse(final String url, final String mediaType, final String payload, final Response.Status expectedStatus) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, USER_ID.toString());
        map.add(HeaderConstants.CLIENT_CORRELATION_ID, randomUUID());

        final String writeUrl = getWriteUrl(url);
        String responseBody = "";
        try (Response response = restClient.postCommand(writeUrl, mediaType, payload, map)) {
            LOGGER.info("Post call made: \n\tURL = {} \n\tMedia type = {} \n\tUser = {}\n",
                    writeUrl, mediaType, USER_ID);
            try {
                responseBody = response.readEntity(String.class);
            } catch (IllegalStateException e) {
                //no-op in case of no response
            }
            assertThat(format("Post returned not expected status code with body: %s", responseBody),
                    response.getStatus(), is(expectedStatus.getStatusCode()));
        }

        return responseBody;
    }

    public static UUID makeMultipartFormPostCall(final UUID userId, final String url, final String fileFieldName, final String fileName) {
        final File file = new File(fileName);
        final UUID correlationId = randomUUID();

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, userId.toString());
        headers.add(HeaderConstants.CLIENT_CORRELATION_ID, correlationId);

        final MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData(fileFieldName, file, MediaType.MULTIPART_FORM_DATA_TYPE, file.getName());
        final GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<>(mdo) {
        };
        try (Response response = RESTEASY_CLIENT.target(getWriteUrl(url)).request().headers(headers).post(
                Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE))) {
            assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        }

        return correlationId;
    }

    public static Response makeGetCall(final String url, final String mediaType) {
        return makeGetCall(url, mediaType, USER_ID);
    }

    public static Response makeGetCall(final String url, final String mediaType, final UUID userId) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
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

    private static String getWriteUrl(final String resource) {
        return WRITE_BASE_URL + resource;
    }

    public static String getReadUrl(final String resource) {
        return READ_BASE_URL + resource;
    }


}
