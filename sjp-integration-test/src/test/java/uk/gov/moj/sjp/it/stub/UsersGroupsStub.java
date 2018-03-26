package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import java.util.UUID;

public class UsersGroupsStub {

    public static final String SJP_PROSECUTORS = "SJP Prosecutors";
    public static final String LEGAL_ADVISERS_GROUP = "Legal Advisers";
    public static final String COURT_ADMINISTRATORS_GROUP = "Court Administrators";

    private static final String USER_GROUPS_ALL_USERS_QUERY_URL = "/usersgroups-query-api/query/api/rest/usersgroups/users/.*/groups";
    private static final String USER_GROUPS_USERS_QUERY_URL = "/usersgroups-query-api/query/api/rest/usersgroups/users/%s/groups";
    private static final String USER_GROUPS_USERS_QUERY_MEDIA_TYPE = "application/vnd.usersgroups.groups+json";
    private static final String USER_GROUPS_USER_DETAILS_QUERY_URL = "/usersgroups-query-api/query/api/rest/usersgroups/users/%s";

    public static void stubAllGroupsForUser(String userId) {
        stubPaylodForAllUsers(getPayload("stub-data/usersgroups.get-groups-by-user-with-all-groups.json"), USER_GROUPS_ALL_USERS_QUERY_URL);
    }

    public static void stubGroupForUser(String userId, String groupName) {
        stubPaylodForUserId(getPayload("stub-data/usersgroups.get-groups-by-user-with-single-group.json")
                .replace("GROUPNAME", groupName), userId, USER_GROUPS_USERS_QUERY_URL);
    }

    public static void stubForUserDetails(final String userId, final String prosecutingAuthorityAccess) {
        stubPaylodForUserId(getPayload("stub-data/usersgroups.user-details-with-prosecuting-authority-access.json")
                .replace("PROSECUTINGAUTHORITYACCESS", prosecutingAuthorityAccess), userId,
                USER_GROUPS_USER_DETAILS_QUERY_URL);
    }

    public static void stubForUserDetails(final String userId) {
        stubPaylodForUserId(getPayload("stub-data/usersgroups.user-details-without-prosecuting-authority-access.json"), userId
                , USER_GROUPS_USER_DETAILS_QUERY_URL);
    }

    private static void stubPaylodForUserId(String responsePayload, String userId, String queryUrl) {
        String url = format(queryUrl, userId);

        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responsePayload)));

        waitForStubToBeReady(url, USER_GROUPS_USERS_QUERY_MEDIA_TYPE);
    }

    private static void stubPaylodForAllUsers(String responsePayload, String url) {

        stubFor(get(urlPathMatching(url))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responsePayload)));

        waitForStubToBeReady(url, USER_GROUPS_USERS_QUERY_MEDIA_TYPE);
    }
}
