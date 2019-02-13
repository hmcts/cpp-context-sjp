package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemDocumentGeneratorStub {

    private static final String SYSTEM_DOCUMENT_GENERATOR_QUERY_URL = "/system-documentgenerator-api/rest/documentgenerator/render";

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocumentGeneratorStub.class);

    private static final String RESPONSE_FILE_NAME = "scrooge-full.pdf";

    public static void stubDocumentGeneratorEndPoint() {
        stubPingFor("system-documentgenerator-api");

        stubFor(post(urlPathMatching(SYSTEM_DOCUMENT_GENERATOR_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM)
                        .withBody(getFileInBytesFromName(RESPONSE_FILE_NAME))));
    }

    private static byte[] getFileInBytesFromName(final String fileName) {
        final String path = "src/test/resources/documents/";
        try {
            return readFileToByteArray(new File(path.concat(fileName)));
        } catch (IOException e) {
            LOGGER.info("IO Exception while reading the file, {}" + RESPONSE_FILE_NAME);
        }

        return null;
    }

    public static List<JSONObject> pollDocumentGenerationRequests(final Matcher<Collection<?>> matcher) {
        try {
            final List<JSONObject> postRequests = await().until(() ->
                    findAll(postRequestedFor(urlPathMatching(SYSTEM_DOCUMENT_GENERATOR_QUERY_URL)))
                            .stream()
                            .map(LoggedRequest::getBodyAsString)
                            .map(JSONObject::new)
                            .collect(toList()), matcher);

            return postRequests;
        } catch (final ConditionTimeoutException timeoutException) {
            LOGGER.info("Exception while finding the captured requests in wire mock:" + timeoutException);
            return emptyList();
        }
    }
}
