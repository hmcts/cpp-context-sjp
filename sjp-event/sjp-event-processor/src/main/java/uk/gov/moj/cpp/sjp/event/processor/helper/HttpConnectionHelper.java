package uk.gov.moj.cpp.sjp.event.processor.helper;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConnectionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnectionHelper.class);

    private static final String CONTENT_TYPE = "content-type";
    private static final String APPLICATION_JSON_CONTENT_TYPE = "application/vnd.courtlistpublishing-service.sjp.post+json";
    private static final String SYSTEM_USER_ID = "abce7b02-7872-4c0d-8ffc-a475fafd2819";

    public Integer getResponseCode(final String url, final String payload) throws IOException {
        final HttpPost post = new HttpPost(url);
        post.addHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE);
        post.addHeader(HeaderConstants.USER_ID, SYSTEM_USER_ID);
        post.setEntity(new StringEntity(payload));

        LOGGER.info("sending POST request to url {}", url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {
            final int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.info("received response from url {}, statusCode {}", url, statusCode);
            return statusCode;
        } catch (final IOException e) {
            LOGGER.error("failed to send POST request to url {}", url, e);
            throw e;
        }
    }
}
