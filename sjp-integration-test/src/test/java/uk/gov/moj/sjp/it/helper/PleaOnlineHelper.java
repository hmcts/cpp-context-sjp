package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.http.HeaderConstants;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PleaOnlineHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleaOnlineHelper.class);

    private String caseId;
    private String defendantId;
    private final String writeUrl;

    public PleaOnlineHelper(CaseSjpHelper caseSjpHelper) {
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
}
