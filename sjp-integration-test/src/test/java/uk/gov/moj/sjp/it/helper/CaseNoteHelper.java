package uk.gov.moj.sjp.it.helper;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class CaseNoteHelper {

    public static void addCaseNote(final UUID caseId, final UUID userId, final String noteText, final String type, final UUID decisionId, final Response.Status expectedStatus) {
        final String url = String.format("/cases/%s/notes", caseId);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("noteText", noteText)
                .add("noteType", type);

        if (nonNull(decisionId)) {
            payloadBuilder.add("decisionId", decisionId.toString());
        }

        makePostCall(userId, url, "application/vnd.sjp.add-case-note+json", payloadBuilder.build().toString(), expectedStatus);
    }

    public static void addCaseNote(final UUID caseId, final UUID userId, final String noteText, final String type, final Response.Status expectedStatus) {
        addCaseNote(caseId, userId, noteText, type, null, expectedStatus);
    }

    public static JsonObject getCaseNotes(final UUID caseId, final UUID userId, final Response.Status expectedStatus) {
        final String url = String.format("/cases/%s/notes", caseId);
        final Response response = makeGetCall(url, "application/vnd.sjp.query.case-notes+json", userId);
        assertThat(response.getStatus(), is(expectedStatus.getStatusCode()));
        return JsonHelper.getJsonObject(response.readEntity(String.class));
    }

    public static JsonObject pollForCaseNotes(final UUID caseId, final Matcher<? super ReadContext> matcher) {
        final String url = String.format("/cases/%s/notes", caseId);
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(url), "application/vnd.sjp.query.case-notes+json")
                .withHeader(HeaderConstants.USER_ID, randomUUID());
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }
}
