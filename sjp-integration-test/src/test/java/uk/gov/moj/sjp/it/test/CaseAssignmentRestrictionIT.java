package uk.gov.moj.sjp.it.test;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.pollCaseAssignmentRestriction;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseAssignmentRestrictionIT extends BaseIntegrationTest {

    private static final User systemUser = user()
            .withUserId(randomUUID())
            .withFirstName("system")
            .withLastName("system")
            .build();
    private static final Random random = new Random();

    @BeforeEach
    public void setUp() throws SQLException {
        cleanViewStore();

        stubForUserDetails(systemUser);
        stubGroupForUser(systemUser.getUserId(), "System Users");
    }

    @Test
    public void shouldAddCaseAssignmentRestriction() {

        final String prosecutingAuthority = "TVL" + random.nextInt(99);
        // To be sure that we create a new record otherwise we can send an update to an existing one and query the old value which fails the test
        addCaseAssignmentRestriction(prosecutingAuthority, emptyList(), emptyList(), "2000-01-01", "2999-08-08");

        pollCaseAssignmentRestriction(prosecutingAuthority, new Matcher[]{
                withJsonPath("$.prosecutingAuthority", is(prosecutingAuthority)),
                withJsonPath("$.includeOnly", hasSize(0)),
                withJsonPath("$.exclude", hasSize(0))
        });


        final String lja1 = String.valueOf(random.nextInt(1000));
        final String lja2 = String.valueOf(random.nextInt(1000));
        final String lja3 = String.valueOf(random.nextInt(1000));
        final String lja4 = String.valueOf(random.nextInt(1000));
        addCaseAssignmentRestriction(prosecutingAuthority, newArrayList(lja1, lja2), newArrayList(lja3, lja4), "2000-01-01", "2999-08-08");

        pollCaseAssignmentRestriction(prosecutingAuthority, new Matcher[]{
                withJsonPath("$.prosecutingAuthority", is(prosecutingAuthority)),
                withJsonPath("$.includeOnly", hasSize(2)),
                withJsonPath("$.includeOnly", containsInAnyOrder(lja3, lja4)),
                withJsonPath("$.exclude", hasSize(2)),
                withJsonPath("$.exclude", containsInAnyOrder(lja1, lja2)),
        });

        addCaseAssignmentRestriction(prosecutingAuthority, newArrayList(lja1, lja2), newArrayList(lja3, lja4), "2000-01-01", "2000-08-08");
        pollCaseAssignmentRestriction(prosecutingAuthority);
    }

    public static void addCaseAssignmentRestriction(final String prosecutingAuthority, final List<String> exclude, final List<String> includeOnly, final String validFrom, final String validTo) {
        final String url = "/case-assignment-restriction";
        final JsonArrayBuilder excludeList = createArrayBuilder();
        exclude.forEach(item -> excludeList.add(item));
        final JsonArrayBuilder includeOnlyList = createArrayBuilder();
        includeOnly.forEach(item -> includeOnlyList.add(item));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("prosecutingAuthority", prosecutingAuthority)
                .add("exclude", excludeList)
                .add("includeOnly", includeOnlyList)
                .add("validFrom", validFrom)
                .add("validTo", validTo);

        makePostCall(systemUser.getUserId(), url, "application/vnd.sjp.add-case-assignment-restriction+json", payloadBuilder.build().toString(), Response.Status.ACCEPTED);
    }
}
