package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemDocumentGeneratorStub {

    private static final String SYSTEM_DOCUMENT_GENERATOR_QUERY_URL = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/render";

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocumentGeneratorStub.class);

    public static void stubDocumentGeneratorEndPoint(final byte[] document) {
        stubFor(post(urlPathMatching(SYSTEM_DOCUMENT_GENERATOR_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM)
                        .withBody(document)));
    }

    public static List<JSONObject> pollDocumentGenerationRequests(final Matcher<Collection<?>> matcher) {
        try {

            return await().pollInterval(200, TimeUnit.MILLISECONDS).
                    until(() ->
                    findAll(postRequestedFor(urlPathMatching(SYSTEM_DOCUMENT_GENERATOR_QUERY_URL)))
                            .stream()
                            .map(LoggedRequest::getBodyAsString)
                            .map(JSONObject::new)
                            .collect(toList()), matcher);
        } catch (final ConditionTimeoutException timeoutException) {
            LOGGER.info("Exception while finding the captured requests in wire mock:" + timeoutException);
            return emptyList();
        }
    }
}
