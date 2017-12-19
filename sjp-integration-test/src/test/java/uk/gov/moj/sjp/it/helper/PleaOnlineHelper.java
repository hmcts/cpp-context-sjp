package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PleaOnlineHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleaOnlineHelper.class);

    private String caseId;
    private String defendantId;
    private final String writeUrl;
    private final MultivaluedMap<String, Object> headers;

    public PleaOnlineHelper(CaseSjpHelper caseSjpHelper) {
        headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, USER_ID);
        this.caseId = caseSjpHelper.getCaseId();
        this.defendantId = caseSjpHelper.getSingleDefendantId();
        writeUrl = String.format("/cases/%s/defendants/%s/plea-online", caseId, defendantId);
    }

    private void pleaOnline(final String payload,
                            final String contentType,
                            final Response.StatusType expectedStatus) {
        LOGGER.info("Request payload: {}", new JsonPath(payload).prettify());
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, USER_ID);
        final Response response = restClient.postCommand(getWriteUrl(writeUrl), contentType, payload, map);
        assertThat(response.getStatus(), equalTo(expectedStatus.getStatusCode()));
    }

    public void pleaOnline(final String payload) {
        pleaOnline(payload, Response.Status.ACCEPTED);
    }

    public void pleaOnline(final String payload, final Response.StatusType expectedStatus) {
        pleaOnline(payload, "application/vnd.sjp.plea-online+json", expectedStatus);
    }

    public Response getOnlinePlea(final String caseId) {
        final String resource = getReadUrl(format("/cases/%s/defendants-online-plea", caseId));
        final String contentType = "application/vnd.sjp.query.defendants-online-plea+json";
        return restClient.query(resource, contentType, headers);
    }

    public String getOnlinePlea(final String caseId, final Matcher jsonMatcher) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> getOnlinePlea(caseId).readEntity(String.class), jsonMatcher);
    }
}
