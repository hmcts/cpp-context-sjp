package uk.gov.moj.sjp.it.test;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

public class CaseAssignmentRestrictionIT extends BaseIntegrationTest{

    private static final User systemUser = user()
            .withUserId(randomUUID())
            .withFirstName("system")
            .withLastName("system")
            .build();
    private static final Random random = new Random();

    @Before
    public void setUp() throws SQLException {
        final SjpDatabaseCleaner cleaner = new SjpDatabaseCleaner();
        cleaner.cleanViewStore();

        stubForUserDetails(systemUser);
        stubGroupForUser(systemUser.getUserId(), "System Users");
    }

    @Test
    public void shouldAddCaseAssignmentRestriction() {

        final String prosecutingAuthority = "TVL" + random.nextInt(99);
        // To be sure that we create a new record otherwise we can send an update to an existing one and query the old value which fails the test
        addCaseAssignmentRestriction(prosecutingAuthority, emptyList(), emptyList());
        JsonObject caseAssignmentRestriction = getCaseAssignmentRestriction(prosecutingAuthority, 0);
        assertThat(caseAssignmentRestriction.getString("prosecutingAuthority"), equalTo(prosecutingAuthority));
        assertThat(caseAssignmentRestriction.getJsonArray("exclude"), hasSize(0));
        assertThat(caseAssignmentRestriction.getJsonArray("includeOnly"), hasSize(0));


        final String lja1 = String.valueOf(random.nextInt(1000));
        final String lja2 = String.valueOf(random.nextInt(1000));
        final String lja3 = String.valueOf(random.nextInt(1000));
        final String lja4 = String.valueOf(random.nextInt(1000));
        addCaseAssignmentRestriction(prosecutingAuthority, newArrayList(lja1, lja2), newArrayList(lja3, lja4));


        caseAssignmentRestriction = getCaseAssignmentRestriction(prosecutingAuthority, 2);
        assertThat(caseAssignmentRestriction.getString("prosecutingAuthority"), equalTo(prosecutingAuthority));
        assertThat(caseAssignmentRestriction.getJsonArray("exclude"), hasSize(2));
        assertThat(caseAssignmentRestriction.getJsonArray("exclude")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(toList()), containsInAnyOrder(lja1, lja2));
        assertThat(caseAssignmentRestriction.getJsonArray("includeOnly"), hasSize(2));
        assertThat(caseAssignmentRestriction.getJsonArray("includeOnly").getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(toList()), containsInAnyOrder(lja3, lja4));
    }

    public static void addCaseAssignmentRestriction(final String prosecutingAuthority, final List<String> exclude, final List<String> includeOnly) {
        final String url = "/case-assignment-restriction";
        final JsonArrayBuilder excludeList = createArrayBuilder();
        exclude.forEach(item -> excludeList.add(item));
        final JsonArrayBuilder includeOnlyList = createArrayBuilder();
        includeOnly.forEach(item -> includeOnlyList.add(item));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("prosecutingAuthority", prosecutingAuthority)
                .add("exclude", excludeList)
                .add("includeOnly", includeOnlyList);

        makePostCall(systemUser.getUserId(), url, "application/vnd.sjp.add-case-assignment-restriction+json", payloadBuilder.build().toString(), Response.Status.ACCEPTED);
    }

    public static JsonObject getCaseAssignmentRestriction(final String prosecutingAuthority, int excludeSize) {
        final String url = String.format("/case-assignment-restriction?prosecutingAuthority=%s", prosecutingAuthority);
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(url), "application/vnd.sjp.query.case-assignment-restriction+json")
                .withHeader(HeaderConstants.USER_ID, systemUser.getUserId());

        return pollWithDefaultsUntilResponseIsJson(requestParams.build(),
                allOf(
                        withJsonPath("$.prosecutingAuthority", is(prosecutingAuthority)),
                        withJsonPath("$.exclude", iterableWithSize(excludeSize))));
    }
}
