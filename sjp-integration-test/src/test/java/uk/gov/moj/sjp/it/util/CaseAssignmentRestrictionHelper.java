package uk.gov.moj.sjp.it.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_LJA_NATIONAL_COURT_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_LJA_NATIONAL_COURT_CODE;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithNotFound;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class CaseAssignmentRestrictionHelper {

    private static final User systemUser = user()
            .withUserId(randomUUID())
            .withFirstName("system")
            .withLastName("system")
            .build();

    public static void provisionCaseAssignmentRestrictions(Set<ProsecutingAuthority> prosecutingAuthorities) {

        stubSystemUser();

        if (prosecutingAuthorities.contains(TFL)) {
            // TFL is london only
            addCaseAssignmentRestriction(TFL.name(), Arrays.asList(DEFAULT_LONDON_LJA_NATIONAL_COURT_CODE), Arrays.asList());
        }
        if (prosecutingAuthorities.contains(TVL)) {
            addCaseAssignmentRestriction(TVL.name(), Arrays.asList(), Arrays.asList());
        }
        if (prosecutingAuthorities.contains(DVLA)) {
            addCaseAssignmentRestriction(DVLA.name(), Arrays.asList(), Arrays.asList());
        }
    }

    public static void stubSystemUser() {
        stubForUserDetails(systemUser);
        stubGroupForUser(systemUser.getUserId(), "System Users");
    }

    public static void addCaseAssignmentRestriction(final String prosecutingAuthority, final List<String> include, final List<String> exclude) {

        final String url = "/case-assignment-restriction";
        final JsonArrayBuilder excludeList = createArrayBuilder();
        exclude.forEach(item -> excludeList.add(item));
        final JsonArrayBuilder includeOnlyList = createArrayBuilder();
        include.forEach(item -> includeOnlyList.add(item));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("prosecutingAuthority", prosecutingAuthority)
                .add("exclude", excludeList)
                .add("includeOnly", includeOnlyList);

        makePostCall(systemUser.getUserId(), url, "application/vnd.sjp.add-case-assignment-restriction+json", payloadBuilder.build().toString(), Response.Status.ACCEPTED);

        // Ensure the restriction exists
        pollCaseAssignmentRestriction(prosecutingAuthority, new Matcher[]{
                withJsonPath("$.prosecutingAuthority", is(prosecutingAuthority)),
                withJsonPath("$.dateTimeCreated", greaterThan(ZonedDateTime.now().minusDays(1).toString()))
        });

    }

    public static JsonObject pollCaseAssignmentRestriction(final String prosecutingAuthority, final Matcher[] matchers) {
        final String url = String.format("/case-assignment-restriction?prosecutingAuthority=%s", prosecutingAuthority);
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(url), "application/vnd.sjp.query.case-assignment-restriction+json")
                .withHeader(HeaderConstants.USER_ID, systemUser.getUserId());

        return pollWithDefaultsUntilResponseIsJson(requestParams.build(),
                allOf(matchers));
    }

    public static void pollCaseAssignmentRestriction(final String prosecutingAuthority) {
        final String url = String.format("/case-assignment-restriction?prosecutingAuthority=%s", prosecutingAuthority);
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(url), "application/vnd.sjp.query.case-assignment-restriction+json")
                .withHeader(HeaderConstants.USER_ID, systemUser.getUserId());

        pollWithNotFound(requestParams.build());
    }
}
